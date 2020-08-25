/*
 * Copyright 2020 FlowSquad GmbH
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package io.flowcov.camunda.util;

import io.flowcov.camunda.model.CoveredElement;

import java.util.Comparator;

/**
 * Compared covered elements by their process definition keys and element IDs.
 */
public class CoveredElementComparator implements Comparator<CoveredElement> {

    private static CoveredElementComparator singleton;

    private CoveredElementComparator() {
    }

    ;

    public static CoveredElementComparator instance() {

        if (singleton == null) {
            singleton = new CoveredElementComparator();
        }

        return singleton;
    }

    @Override
    public int compare(CoveredElement o1, CoveredElement o2) {

        if (o1 == null && o2 == null) {
            return 0;
        } else if (o1 == null) {
            return -1;
        } else if (o2 == null) {
            return 1;
        }

        final int processDefinitionIdComparison =
                o1.getProcessDefinitionKey()
                        .compareTo(o2.getProcessDefinitionKey());

        if (processDefinitionIdComparison == 0) {
            return o1.getElementId().compareTo(o2.getElementId());
        }

        return processDefinitionIdComparison;
    }

}
