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

package de.gematik.tim.test.glue.api.message;

import static de.gematik.tim.test.glue.api.ActorMemoryKeys.DIRECT_CHAT_NAME;
import static de.gematik.tim.test.glue.api.ActorMemoryKeys.MX_ID;
import static de.gematik.tim.test.glue.api.TestdriverApiEndpoint.SEND_DIRECT_MESSAGE;
import static de.gematik.tim.test.glue.api.room.UseRoomAbility.addRoomToActor;
import static de.gematik.tim.test.glue.api.room.questions.GetRoomQuestion.ownRoom;
import static de.gematik.tim.test.glue.api.utils.GlueUtils.createUniqueMessageText;
import static de.gematik.tim.test.glue.api.utils.GlueUtils.isSameHomeserver;
import static de.gematik.tim.test.glue.api.utils.TestcasePropertiesManager.addMessage;
import static net.serenitybdd.rest.SerenityRest.lastResponse;
import static org.apache.commons.lang3.StringUtils.isNotBlank;

import de.gematik.tim.test.glue.api.rawdata.RawDataStatistics;
import de.gematik.tim.test.models.DirectMessageDTO;
import de.gematik.tim.test.models.MessageDTO;
import de.gematik.tim.test.models.RoomDTO;
import java.util.List;
import lombok.RequiredArgsConstructor;
import net.serenitybdd.screenplay.Actor;
import net.serenitybdd.screenplay.Task;
import org.springframework.http.HttpStatus;

@RequiredArgsConstructor
public class SendDirectMessageTask implements Task {

  private final Actor toActor;
  private final String message;

  public static SendDirectMessageTask sendDirectMessageTo(Actor toActor, String message) {
    return new SendDirectMessageTask(toActor, message);
  }

  @Override
  public <T extends Actor> void performAs(T actor) {
    String actorMxId = actor.recall(MX_ID);
    String toMxId = toActor.recall(MX_ID);

    DirectMessageDTO directMessage = new DirectMessageDTO()
        .body(createUniqueMessageText())
        .msgtype("m.text")
        .toAccount(toMxId);
    actor.attemptsTo(SEND_DIRECT_MESSAGE.request().with(req -> req.body(directMessage)));
    if (HttpStatus.valueOf(lastResponse().statusCode()).is2xxSuccessful()) {
      addMessage(this.message, lastResponse().as(MessageDTO.class));
    }

    logEventsAndSaveRoomToActor(actor, actorMxId, toMxId);

  }

  private <T extends Actor> void logEventsAndSaveRoomToActor(T actor, String actorMxId,
      String toMxId) {
    boolean roomDoesExist =
        isNotBlank(actor.recall(DIRECT_CHAT_NAME + toMxId)) || isNotBlank(
            this.toActor.recall(DIRECT_CHAT_NAME + actorMxId));

    boolean requestSuccessful = lastResponse().getStatusCode() == 200;
    if (isSameHomeserver(toMxId, actorMxId)) {
      if (!roomDoesExist) {
        RawDataStatistics.inviteToRoomSameHomeserver();
      }
      if (requestSuccessful) {
        RawDataStatistics.exchangeMessageSameHomeserver();
      }
    } else {
      if (!roomDoesExist) {
        RawDataStatistics.inviteToRoomMultiHomeserver();
      }
      if (requestSuccessful) {
        RawDataStatistics.exchangeMessageMultiHomeserver();
      }
    }
    if (requestSuccessful && !roomDoesExist) {
      handleNewRoom(actor, actorMxId, toMxId);
    }
  }

  private void handleNewRoom(Actor actor, String actorMxId, String toMxId) {
    RoomDTO room = actor.asksFor(
        ownRoom().withMembers(List.of(actorMxId, toMxId)));
    String roomNameActor = DIRECT_CHAT_NAME + toMxId;
    String roomNameToActor = DIRECT_CHAT_NAME + actorMxId;
    actor.remember(roomNameActor, room.getName());
    toActor.remember(roomNameToActor, room.getName());
    addRoomToActor(roomNameActor, room, actor);
    addRoomToActor(roomNameToActor, room, toActor);
  }
}
