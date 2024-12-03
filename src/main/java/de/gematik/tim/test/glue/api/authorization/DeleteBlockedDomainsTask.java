/*
 *
 *  * Copyright 2024 gematik GmbH
 *  *
 *  * Licensed under the Apache License, Version 2.0 (the "License");
 *  * you may not use this file except in compliance with the License.
 *  * You may obtain a copy of the License at
 *  *
 *  *     http://www.apache.org/licenses/LICENSE-2.0
 *  *
 *  * Unless required by applicable law or agreed to in writing, software
 *  * distributed under the License is distributed on an "AS IS" BASIS,
 *  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  * See the License for the specific language governing permissions and
 *  * limitations under the License.
 *
 */

package de.gematik.tim.test.glue.api.authorization;

import static de.gematik.tim.test.glue.api.TestdriverApiEndpoint.DELETE_BLOCKED_DOMAINS;

import de.gematik.tim.test.models.AuthorizationListDTO;
import de.gematik.tim.test.models.DomainDTO;
import java.util.List;
import lombok.RequiredArgsConstructor;
import net.serenitybdd.screenplay.Actor;
import net.serenitybdd.screenplay.Task;

@RequiredArgsConstructor
public class DeleteBlockedDomainsTask implements Task {

  private final List<String> blockedDomains;

  public static DeleteBlockedDomainsTask deleteBlockedDomains(List<String> blockedDomains) {
    return new DeleteBlockedDomainsTask(blockedDomains);
  }

  @Override
  public <T extends Actor> void performAs(T actor) {
    List<DomainDTO> domainDTOs =
        blockedDomains.stream().map(domain -> new DomainDTO().domain(domain)).toList();
    AuthorizationListDTO blockedList = new AuthorizationListDTO();
    blockedList.domains(domainDTOs);
    actor.attemptsTo(DELETE_BLOCKED_DOMAINS.request().with(req -> req.body(blockedList)));
  }
}
