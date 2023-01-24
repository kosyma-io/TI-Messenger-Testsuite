/*
 * Copyright (c) 2023 gematik GmbH
 * 
 * Licensed under the Apache License, Version 2.0 (the License);
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *     http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an 'AS IS' BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package de.gematik.tim.test.glue.api.fhir.practitioner;

import static de.gematik.tim.test.glue.api.ActorMemoryKeys.MX_ID;
import static de.gematik.tim.test.glue.api.account.AccountQuestion.ownAccountInfos;
import static de.gematik.tim.test.glue.api.fhir.practitioner.FhirSearchQuestion.practitionerInFhirDirectory;
import static java.util.Objects.requireNonNull;
import static net.serenitybdd.screenplay.actors.OnStage.theActorCalled;
import static net.serenitybdd.screenplay.actors.OnStage.theActorInTheSpotlight;
import static net.serenitybdd.screenplay.rest.questions.ResponseConsequence.seeThatResponse;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.collection.IsEmptyCollection.empty;

import de.gematik.tim.test.models.FhirPractitionerDTO;
import de.gematik.tim.test.models.FhirPractitionerSearchResultDTO;
import io.cucumber.java.de.Dann;
import io.cucumber.java.de.Wenn;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import java.util.List;
import net.serenitybdd.screenplay.Actor;

public class FhirSearchGlue {

  // MXID search
  @When("{string} can find {listOfStrings} in directory")
  @Wenn("{string} findet {listOfStrings} in FHIR")
  public void findUser(String actorName, List<String> actorNames) {
    Actor actor1 = theActorCalled(actorName);
    actorNames.forEach(a -> {
      Actor actorToFind = theActorCalled(a);
      FhirPractitionerSearchResultDTO response = actor1.asksFor(
          practitionerInFhirDirectory().withMxid(actorToFind.recall(MX_ID)));
      assertThat(response).isNotNull();
      assertThat(response.getTotalSearchResults()).isEqualTo(1);
      assertThat(requireNonNull(response.getSearchResults()).get(0).getMxid()).isEqualTo(
          actorToFind.recall(MX_ID));
    });
  }

  @When("{string} can not find {string}")
  @Wenn("{string} kann {string} nicht finden in FHIR")
  public void dontFindPractitionerInFhir(String actorName, String userName) {
    FhirSearchQuestion question = practitionerInFhirDirectory()
        .withMxid(theActorCalled(userName).recall(MX_ID));
    dontFindPractitionerInFhir(question, actorName);
  }

  @Wenn("{string} kann {string} nicht finden in FHIR [Retry {long} - {long}]")
  public void dontFindPractitionerInFhir(String actorName, String userName, Long timeout,
      Long pollInterval) {
    FhirSearchQuestion question = practitionerInFhirDirectory()
        .withMxid(theActorCalled(userName).recall(MX_ID))
        .withCustomInterval(timeout, pollInterval);
    dontFindPractitionerInFhir(question, actorName);
  }

  private void dontFindPractitionerInFhir(FhirSearchQuestion question, String actorName) {
    Actor actor1 = theActorCalled(actorName);
    FhirPractitionerSearchResultDTO response = actor1.asksFor(question);
    assertThat(response).isNotNull();
    assertThat(response.getTotalSearchResults()).isEqualTo(0);
  }

  // Parameter search
  @When("{string} can find {string} by searching by name with cut {int}-{int} (amount front-back) char(s)")
  @Wenn("{string} findet TI-Messenger-Nutzer {string} bei Suche nach Namen minus {int}-{int} \\(Anzahl vorne-hinten) Char\\(s) abgeschnitten")
  public void findUserWithSearchParam(String actorName, String userName, int begin, int end) {
    Actor actor = theActorCalled(actorName);
    Actor searchedActor = theActorCalled(userName);
    String displayName = requireNonNull(searchedActor.asksFor(ownAccountInfos()).getDisplayName());

    assertThat(begin + end).as("You cut to much of the displayname: " + displayName)
        .isLessThan(displayName.length());
    String cutName = displayName.substring(begin, displayName.length() - end);

    FhirPractitionerSearchResultDTO response = actor.asksFor(
        practitionerInFhirDirectory().withName(cutName));
    assertThat(response).extracting(FhirPractitionerSearchResultDTO::getSearchResults).asList()
        .map(FhirPractitionerDTO.class::cast)
        .filteredOn(m -> requireNonNull(m.getMxid()).equals(searchedActor.recall(MX_ID)))
        .hasSize(1);
  }

  @When("{string} can NOT find {string} by searching by name {string}")
  @Dann("{string} findet TI-Messenger-Nutzer {string} bei Suche mit Namen {string} NICHT")
  public void dontFindUserWithName(String actorName, String userName, String value) {
    Actor actor = theActorCalled(actorName);
    String actor2Id = theActorCalled(userName).recall(MX_ID);
    FhirPractitionerSearchResultDTO response = actor.asksFor(
        practitionerInFhirDirectory().withName(value));
    assertThat(response).extracting(FhirPractitionerSearchResultDTO::getSearchResults).asList()
        .map(FhirPractitionerDTO.class::cast)
        .filteredOn(m -> requireNonNull(m.getMxid()).equals(actor2Id)).hasSize(0);
  }

  // Conditions
  @Then("practitioner information is returned")
  public void practitionerInformationIsReturned() {
    theActorInTheSpotlight().should(seeThatResponse("Search practitioner",
        res -> res.statusCode(200)
            .body("searchResults", not(empty()))));
  }
}
