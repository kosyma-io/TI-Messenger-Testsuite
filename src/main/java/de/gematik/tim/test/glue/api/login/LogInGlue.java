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

package de.gematik.tim.test.glue.api.login;

import static de.gematik.tim.test.glue.api.ActorMemoryKeys.IS_ORG_ADMIN;
import static de.gematik.tim.test.glue.api.GeneralStepsGlue.checkResponseCode;
import static de.gematik.tim.test.glue.api.devices.ClientKind.MESSENGER_CLIENT;
import static de.gematik.tim.test.glue.api.devices.ClientKind.ORG_ADMIN;
import static de.gematik.tim.test.glue.api.devices.ClientKind.PRACTITIONER;
import static de.gematik.tim.test.glue.api.devices.DevicesControllerGlue.checkIs;
import static de.gematik.tim.test.glue.api.login.LoginTask.login;
import static de.gematik.tim.test.glue.api.login.LogoutTask.logout;
import static net.serenitybdd.screenplay.actors.OnStage.theActorCalled;
import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.OK;

import io.cucumber.java.de.Dann;
import io.cucumber.java.de.Wenn;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import net.serenitybdd.screenplay.Actor;

import java.util.List;

public class LogInGlue {

  @When("{string} logs out")
  @Wenn("{string} loggt sich im TI-Messenger aus")
  public void logsOut(String actorName) {
    theActorCalled(actorName).attemptsTo(logout());
    checkResponseCode(actorName, OK.value());
  }

  @When("{string} logs in")
  @Wenn("{string} loggt sich im TI-Messenger ein")
  public static void logsIn(String actorName) {
    theActorCalled(actorName).attemptsTo(login());
    checkResponseCode(actorName, OK.value());
  }

  @Wenn("{string} sich als HBA-User einloggt")
  public void logsInAsHbaUser(String actorName) {
    Actor actor = theActorCalled(actorName);
    logsIn(actorName);
    checkIs(actor, List.of(MESSENGER_CLIENT, PRACTITIONER));
  }

  @Wenn("{string} sich als Org-User einloggt")
  public void logsInAsOrgUser(String actorName) {
    Actor actor = theActorCalled(actorName);
    logsIn(actorName);
    checkIs(actor, List.of(MESSENGER_CLIENT));
  }

  @Wenn("{string} sich als OrgAdmin einloggt")
  public void sichAlsOrgAdminRegistriert(String actorName) {
    Actor actor = theActorCalled(actorName);
    actor.remember(IS_ORG_ADMIN, true);
    logsIn(actorName);
    checkIs(actor, List.of(ORG_ADMIN));
  }

  @Then("registration successful for {string}")
  @Dann("ist das Login für {string} erfolgreich")
  public static void loginSuccess(String actorName) {
    checkResponseCode(actorName, OK.value());
  }

  @Then("registration failed for {string}")
  @Dann("schlägt das Login für {string} fehl")
  public static void loginFailure(String actorName) {
    checkResponseCode(actorName, BAD_REQUEST.value());
  }
}
