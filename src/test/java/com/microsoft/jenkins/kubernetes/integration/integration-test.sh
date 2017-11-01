#!/usr/bin/env bash

show_help() {
    cat <<EOF
In the project root directory, run the following command and pass in the service principal details in parameters.

    bash src/test/java/com/microsoft/jenkins/kubernetes/integration/integration-test.sh

    Prerequisites:
        * Logged in Azure CLI
        * kubectl in PATH

It will create a resource group, create ACS with Kubernetes as orchestrator, together with an ACR
instance in the resource group, and then start the tests. When the tests finishes, it will clean up the resource group created.

To simply the clean up process, all the resources will be created in the same resource group.
You can also pass in the existing Kubernetes master host and docker registry information and the script will reuse existing one.
If the resource group isn't created by the script, it will not be deleted after the tests.
EOF
    exit 0
}

if [[ ! -f pom.xml ]]; then
    show_help
fi

if ! which kubectl; then
    echo "kubectl is not present in \$PATH" >&2
    exit -1
fi

if ! az account show; then
    echo "Azure CLI is not logged in" >&2
    exit -1
fi

set -ex

export SKIP_CLEAN=false

while [[ $# -gt 0 ]]; do
    key="$1"

    case "$key" in
        -h|--master-host)
            export KUBERNETES_CD_MASTER_HOST="$2"
            shift; shift
            ;;
        -u|--admin-user)
            export KUBERNETES_CD_ADMIN_USER="$2"
            shift; shift
            ;;
        -k|--ssh-key-file)
            export KUBERNETES_CD_KEY_PATH="$2"
            shift; shift
            ;;
        --docker-registry)
            export KUBERNETES_CD_DOCKER_REGISTRY="$2"
            shift; shift
            ;;
        --docker-username)
            export KUBERNETES_CD_DOCKER_USERNAME="$2"
            shift; shift
            ;;
        --docker-password)
            export KUBERNETES_CD_DOCKER_PASSWORD="$2"
            shift; shift
            ;;
        --docker-repository)
            export KUBERNETES_CD_DOCKER_REPOSITORY="$2"
            shift; shift
            ;;
        --skip-clean)
            export SKIP_CLEAN=true
            shift
            ;;
        *)
            show_help
            ;;
    esac
done

# common suffix for the names
suffix=$(xxd -p -l 4 /dev/urandom)

resource_group=
if [[ -z "$KUBERNETES_CD_MASTER_HOST" ]] || [[ -z "$KUBERNETES_CD_DOCKER_REPOSITORY" ]]; then
    resource_group="k8s-test-$suffix"
fi

export integration_test_script_pid=$$
export temp_config_dir=kubeconfig.tmp

post_clean_up() {
    set +x
    # wait the testing process to finish
    while ps -p "$integration_test_script_pid" >/dev/null; do
        sleep 0.5
    done
    set -x

    if [[ -d "$temp_config_dir" ]]; then
        echo "Clean up temporary directory $temp_config_dir"
        rm -rf -- "$temp_config_dir"
    fi

    if [[ -n "$resource_group" ]]; then
        echo "Clean up resource group $resource_group"
        az group delete --yes --no-wait --name "$resource_group"
    fi
}

if [[ -n "$resource_group" ]]; then
    az group create --name "$resource_group" --location SoutheastAsia

    if [[ "$SKIP_CLEAN" != "true" ]]; then
        echo "Registering the clean up process..."
        post_clean_up & disown
    fi

    if [[ -z "$KUBERNETES_CD_MASTER_HOST" ]]; then
        k8s_name="k8s-$suffix"
        az acs create --orchestrator-type kubernetes --resource-group "$resource_group" --name "$k8s_name" --agent-count 2 &
        k8s_pid=$!
    fi

    if [[ -z "$KUBERNETES_CD_DOCKER_REPOSITORY" ]]; then
        acr_name="acr$suffix"
        az acr create --resource-group "$resource_group" --name "$acr_name" --sku Basic --admin-enabled true
        export KUBERNETES_CD_DOCKER_REPOSITORY="$(az acr show --resource-group "$resource_group" --name "$acr_name" --query "loginServer" --output tsv)"
        export KUBERNETES_CD_DOCKER_USERNAME="$acr_name"
        export KUBERNETES_CD_DOCKER_PASSWORD="$(az acr credential show --name "$acr_name" --query "passwords[0].value" --output tsv)"
        export KUBERNETES_CD_DOCKER_REGISTRY="http://$KUBERNETES_CD_DOCKER_REPOSITORY"

        docker pull nginx
        docker login -u "$KUBERNETES_CD_DOCKER_USERNAME" -p "$KUBERNETES_CD_DOCKER_PASSWORD" "$KUBERNETES_CD_DOCKER_REGISTRY"
        docker tag nginx:latest "$KUBERNETES_CD_DOCKER_REPOSITORY/kubernetes-test"
        docker push "$KUBERNETES_CD_DOCKER_REPOSITORY/kubernetes-test"
    fi


    if [[ -n "$k8s_pid" ]]; then
        wait "$k8s_pid"

        export KUBERNETES_CD_MASTER_HOST="$(az acs show --resource-group "$resource_group" --name "$k8s_name" --query "masterProfile.fqdn" --output tsv)"
        export KUBERNETES_CD_ADMIN_USER="$(az acs show --resource-group "$resource_group" --name "$k8s_name" --query "linuxProfile.adminUsername" --output tsv)"

        mkdir -p $temp_config_dir
        tmpfile="$(mktemp "$temp_config_dir/kubeconfig.XXXXXX")"

        set +x

        echo -n "Fetch the ACS Kubernetes credentials to $tmpfile "
        k8s_ready=0
        for i in $(seq 60); do
            if az acs kubernetes get-credentials --resource-group "$resource_group" --name "$k8s_name" --file "$tmpfile" >/dev/null 2>&1; then
                if [[ -s $tmpfile ]]; then
                    k8s_ready=1
                    break
                fi
            fi
            echo -n '.'
            sleep 5
        done
        echo

        if [[ "$k8s_ready" != '1' ]]; then
            echo "Timeout while fetching the ACS Kubernetes credentials" >&2
            exit 1
        fi

        echo -n "Wait for the K8s cluster to be ready "
        for i in $(seq 60); do
            if kubectl --kubeconfig "$tmpfile" get pods >/dev/null 2>&1; then
                k8s_ready=2
                break
            fi
            echo -n '.'
            sleep 5
        done
        echo

        if [[ "$k8s_ready" != '2' ]]; then
            echo "Timeout while waiting for the ACS Kubernetes cluster to be ready" >&2
            exit 1
        fi

        set -x
    fi
fi

mvn clean test-compile failsafe:integration-test failsafe:verify

