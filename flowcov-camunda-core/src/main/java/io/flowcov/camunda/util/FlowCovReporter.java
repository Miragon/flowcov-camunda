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

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import io.flowcov.camunda.api.Build;
import lombok.val;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.time.LocalDateTime;

/**
 * Util generating JSON process test coverage reports.
 */
public class FlowCovReporter {


    /**
     * Generates the FlowCov Json report.
     *
     * @param reportPath
     * @param run
     * @throws IOException
     */
    public static void generateReport(
            String reportPath,
            Build run) throws IOException {

        Gson gson = new GsonBuilder()
                .setPrettyPrinting()
                .registerTypeAdapter(LocalDateTime.class, new LocalDateAdapter())
                .create();
        
        val reportJson = gson.toJson(run);
        writeToFile(reportPath, reportJson);

    }

    /**
     * Write the html report.
     *
     * @param filePath
     * @param html
     * @throws IOException
     */
    protected static void writeToFile(String filePath, String html) throws IOException {
        FileUtils.writeStringToFile(new File(filePath), html);
    }


}
