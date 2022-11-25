/*
 * Copyright (c) 2022 gematik GmbH
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

package de.gematik.tim.test.glue.api.devices;

import static de.gematik.tim.test.glue.api.TestdriverApiEndpoint.UNCLAIM_DEVICE;
import static de.gematik.tim.test.glue.api.TestdriverApiPath.DEVICE_ID_VARIABLE;
import static java.util.Objects.nonNull;

import de.gematik.tim.test.glue.api.TestdriverApiAbility;
import de.gematik.tim.test.glue.api.account.CanDeleteAccountAbility;
import de.gematik.tim.test.glue.api.fhir.organisation.healthcareservice.UseHealthcareServiceAbility;
import de.gematik.tim.test.glue.api.fhir.practitioner.CanDeleteOwnMxidAbility;
import io.restassured.specification.RequestSpecification;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.serenitybdd.screenplay.Actor;
import net.serenitybdd.screenplay.HasTeardown;
import net.serenitybdd.screenplay.RefersToActor;

@Slf4j
@Getter
@RequiredArgsConstructor
public class UseDeviceAbility implements TestdriverApiAbility, HasTeardown, RefersToActor {

  private final long deviceId;
  private Actor actor;

  public static UseDeviceAbility useDevice(long deviceId) {
    return new UseDeviceAbility(deviceId);
  }

  public static UseDeviceAbility as(Actor actor) {
    return actor.abilityTo(UseDeviceAbility.class);
  }

  @Override
  public RequestSpecification apply(RequestSpecification requestSpecification) {
    return requestSpecification.pathParam(DEVICE_ID_VARIABLE, deviceId);
  }

  @Override
  public void tearDown() {
    CanDeleteOwnMxidAbility delOwnFhirIdAbility = actor.abilityTo(CanDeleteOwnMxidAbility.class);
    if (nonNull(delOwnFhirIdAbility)) {
      delOwnFhirIdAbility.tearDown();
    }
    CanDeleteAccountAbility ability = actor.abilityTo(CanDeleteAccountAbility.class);
    if (nonNull(ability)) {
      ability.tearDown();
    }
    UseHealthcareServiceAbility hsAbility = actor.abilityTo(UseHealthcareServiceAbility.class);
    if (nonNull(hsAbility)) {
      hsAbility.tearDown();
    }
    UNCLAIM_DEVICE.request().performAs(actor);
  }

  @Override
  public UseDeviceAbility asActor(Actor actor) {
    this.actor = actor;
    return this;
  }
}
