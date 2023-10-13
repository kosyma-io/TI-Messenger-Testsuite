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

import static de.gematik.tim.test.glue.api.ActorMemoryKeys.MX_ID;
import static de.gematik.tim.test.glue.api.TestdriverApiEndpoint.SEND_MESSAGE;
import static de.gematik.tim.test.glue.api.room.questions.GetCurrentRoomQuestion.currentRoom;
import static de.gematik.tim.test.glue.api.utils.GlueUtils.createUniqueMessageText;
import static de.gematik.tim.test.glue.api.utils.GlueUtils.isSameHomeserver;
import static de.gematik.tim.test.glue.api.utils.TestcasePropertiesManager.addMessage;
import static java.util.Objects.requireNonNull;
import static net.serenitybdd.rest.SerenityRest.lastResponse;

import de.gematik.tim.test.glue.api.rawdata.RawDataStatistics;
import de.gematik.tim.test.models.MessageContentDTO;
import de.gematik.tim.test.models.MessageContentInfoDTO;
import de.gematik.tim.test.models.MessageDTO;
import lombok.RequiredArgsConstructor;
import net.serenitybdd.screenplay.Actor;
import net.serenitybdd.screenplay.Task;
import org.springframework.http.HttpStatus;

@RequiredArgsConstructor
public class SendMessageTask implements Task {

  private final String body;
  private String msgType = "m.text";
  private String fileId;
  private String mimetype;
  private Integer size;

  public static SendMessageTask sendMessage(String messageText) {
    requireNonNull(messageText);
    return new SendMessageTask(messageText);
  }

  public SendMessageTask withFileId(String fileId) {
    this.fileId = fileId;
    return this;
  }

  public SendMessageTask withMimetype(String mimetype) {
    this.mimetype = mimetype;
    return this;
  }

  public SendMessageTask withSize(Integer size) {
    this.size = size;
    return this;
  }

  public SendMessageTask withMsgType(String msgType) {
    this.msgType = msgType;
    return this;
  }

  @Override
  public <T extends Actor> void performAs(T actor) {
    MessageContentDTO message = buildMessage();

    actor.attemptsTo(SEND_MESSAGE.request()
        .with(req -> req.body(message)));

    if (HttpStatus.valueOf(lastResponse().statusCode()).is2xxSuccessful()) {
      addMessage(body, lastResponse().as(MessageDTO.class));
    }
    String actorId = actor.recall(MX_ID);
    requireNonNull(actor.asksFor(currentRoom()).getMembers()).forEach(m -> {
      String memberId = requireNonNull(m.getMxid());
      if (actorId.equals(memberId)) {
        return;
      }
      if (isSameHomeserver(actorId, memberId)) {
        RawDataStatistics.exchangeMessageSameHomeserver();
      } else {
        RawDataStatistics.exchangeMessageMultiHomeserver();
      }
    });

  }

  private MessageContentDTO buildMessage() {
    MessageContentDTO message = new MessageContentDTO()
        .body(createUniqueMessageText())
        .msgtype(msgType);
    if (fileId != null) {
      message.fileId(fileId);
      message.info(new MessageContentInfoDTO()
          .mimetype(mimetype)
          .size(size));
    }
    return message;
  }
}
