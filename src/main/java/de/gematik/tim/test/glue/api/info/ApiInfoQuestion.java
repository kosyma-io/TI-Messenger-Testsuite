/*
 * Copyright 2023 gematik GmbH
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
 * limitations under the License.
 */

package de.gematik.tim.test.glue.api.info;

import static de.gematik.tim.test.glue.api.TestdriverApiEndpoint.GET_INFO;
import static de.gematik.tim.test.glue.api.devices.UseDeviceAbility.TEST_CASE_ID_HEADER;
import static de.gematik.tim.test.glue.api.threading.ParallelExecutor.parallelClient;
import static de.gematik.tim.test.glue.api.utils.RequestResponseUtils.parseResponse;
import static de.gematik.tim.test.glue.api.utils.TestcasePropertiesManager.getTestcaseId;

import de.gematik.tim.test.glue.api.threading.Parallel;
import de.gematik.tim.test.glue.api.threading.ActorsNotes;
import de.gematik.tim.test.glue.api.utils.ParallelUtils;
import de.gematik.tim.test.glue.api.exceptions.TestRunException;
import de.gematik.tim.test.models.InfoObjectDTO;
import java.io.IOException;
import net.serenitybdd.screenplay.Actor;
import net.serenitybdd.screenplay.Question;
import okhttp3.Call;
import okhttp3.Response;

public class ApiInfoQuestion implements Question<InfoObjectDTO>, Parallel<InfoObjectDTO> {

  public static ApiInfoQuestion apiInfo() {
    return new ApiInfoQuestion();
  }

  @Override
  public InfoObjectDTO answeredBy(Actor actor) {
    actor.attemptsTo(
        GET_INFO.request().with(res -> res.header(TEST_CASE_ID_HEADER, getTestcaseId())));
    return parseResponse(InfoObjectDTO.class);
  }

  @Override
  public InfoObjectDTO parallel(ActorsNotes notes) {
    Call call = parallelClient().get().newCall(GET_INFO.parallelRequest(notes).build());
    try (Response response = call.execute()) {
      return ParallelUtils.fromJson(response.body().string(), InfoObjectDTO.class);
    } catch (IOException e) {
      throw new TestRunException(e);
    }
  }
}
