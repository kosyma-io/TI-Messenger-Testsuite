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

package de.gematik.tim.test.glue.api.fhir.organisation.location;

import static de.gematik.tim.test.glue.api.TestdriverApiPath.LOCATION_ID_VARIABLE;
import static de.gematik.tim.test.glue.api.fhir.organisation.location.UseLocationAbility.LocationInfo;
import static java.util.Objects.requireNonNull;

import de.gematik.tim.test.glue.api.MultiTargetAbility;
import de.gematik.tim.test.glue.api.TestdriverApiAbility;
import io.restassured.specification.RequestSpecification;

public class UseLocationAbility extends MultiTargetAbility<String, LocationInfo>
    implements TestdriverApiAbility {

  @Override
  public RequestSpecification apply(RequestSpecification requestSpecification) {
    String locationId = getActive().locationId();
    requireNonNull(locationId);
    return requestSpecification.pathParam(LOCATION_ID_VARIABLE, locationId);
  }

  protected record LocationInfo(String locationId, String hsName) {}
}
