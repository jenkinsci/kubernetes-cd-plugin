/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */

Behaviour.specify('select[name$=.credentialsType]', 'hide-block-bound-to-credentials-type', 1000, function(select) {
    var set = ['kubeConfig', 'ssh', 'textCredentials'];

    function handleChange() {
        var value = $(select).getValue();
        var table = $(select).up('table');
        if (!table) {
            return;
        }

        var show = "kubeConfig";
        if (value === 'Text') {
            show = "textCredentials";
        } else if (value === 'SSH') {
            show = "ssh";
        }

        setGroupVisibility(table, show)
    }

    function setGroupVisibility(table, show) {
        for (var i = 0, len = set.length; i < len; ++i) {
            var name = set[i];
            var fieldTable = table.select('table[data-field="' + name + '"]');
            if (fieldTable) {
                fieldTable = fieldTable[0];
            }
            if (!fieldTable) {
                continue;
            }
            if (name === show) {
                fieldTable.show();
            } else {
                fieldTable.hide();
            }
        }
    }

    handleChange();
    $(select).on('change', handleChange);
});
