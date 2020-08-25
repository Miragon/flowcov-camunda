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

package io.flowcov.camunda.junit.rules;

import io.flowcov.camunda.model.ProcessCoverage;
import org.hamcrest.BaseMatcher;
import org.hamcrest.Description;

public class MinimalCoverageMatcher extends BaseMatcher<Double> {

    double minimalCoverage;

    public MinimalCoverageMatcher(double minimalCoveragePercentage) {
        if (0 > minimalCoveragePercentage || minimalCoveragePercentage > 1) {
            throw new RuntimeException("ILLEGAL TEST CONFIGURATION: minimal coverage percentage must be between 0.0 and 1.0 (was " + minimalCoveragePercentage + ")");
        }
        this.minimalCoverage = minimalCoveragePercentage;
    }

    @Override
    public void describeTo(Description description) {
        description.appendText("matches if the coverage ratio is at least ").appendValue(minimalCoverage);
    }

    @Override
    public boolean matches(Object item) {
        return actualPercentage(item) >= minimalCoverage;
    }

    private double actualPercentage(Object item) {
        if (item instanceof ProcessCoverage) {
            return ((ProcessCoverage) item).getCoveragePercentage();
        }
        if (item instanceof Number) {
            return (double) ((Number) item).doubleValue();
        }
        return -1.0;
    }

    @Override
    public void describeMismatch(Object item, Description mismatchDescription) {
        if (item instanceof Number || item instanceof ProcessCoverage) {
            mismatchDescription.appendText("coverage of ").appendValue(actualPercentage(item));
            mismatchDescription.appendText(" is too low)");
            // TODO describe diff of actual and expected items
        } else {
            mismatchDescription.appendValue(item).appendText("is not a Number or Coverage");
        }

    }

}
