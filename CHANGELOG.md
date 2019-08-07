# Kubernetes Continuous Deploy Plugin Changelog

## Version 2.0.0, 2019-08-07
* Change kubernetes sdk to the official one
* Make resources compatible with several api versions
* Support more resource types: StatefulSets, Network policy, Persistent Volume, Persistent Volume Claim
* Enable incremental builds for PRs

## Version 1.0.0, 2019-05-31
**This version forces updating Kubernetes yaml files' api version**
* Bump Jenkins version to 2.60.3
* Upgrade kubernetes-client sdk version to 4.0.4
* Add support for CronJob and HPA

## Version 0.2.3, 2018-06-08
* Documentation and AI fix

## Version 0.2.2, 2018-05-18
* Support for namespace creation and update
* Fix EnvironmentInjector serialization (JENKINS-51147)

## Version 0.2.1, 2018-04-20
* Fix scoped SSH credentials lookup in kubeconfig credentials (#26)
* Fix Kubernetes deploy configuration verification (#29)
* Add support for ConfigMap (#30)
* Fix serialization of 3rd party exceptions thrown from slave (JENKINS-50760)

## Version 0.2.0, 2018-04-03
* Configure kubeconfig in the Jenkins credentials store instead of the job configuration (JENKINS-49781)

   The original "Kubernetes Cluster Credentials" configuration is deprecated.
* Upgrade Kubernetes Client to 3.1.10
* Use scoped credentials lookup (#19)

## Version 0.1.5, 2018-02-22
* Abort build on error (JENKINS-48662 / #12)
* Update Kubernetes Client to 3.1.7

## Version 0.1.4, 2017-11-07
* Fix master node SSH password login on Jenkins slave
* Add Third Party Notice

## Version 0.1.3, 2017-10-10
* Remove EULA

## Version 0.1.2, 2017-09-29
* Fixed a stream closed issue when variable substitution is disabled

## Version 0.1.1, 2017-09-28
* Fixed an issue that plugin crashes on fastxml load

## Version 0.1.0, 2017-09-27
* Initial release
