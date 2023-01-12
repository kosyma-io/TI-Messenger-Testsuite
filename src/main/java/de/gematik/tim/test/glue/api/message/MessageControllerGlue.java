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

package de.gematik.tim.test.glue.api.message;

import static de.gematik.tim.test.glue.api.ActorMemoryKeys.DIRECT_CHAT_NAME;
import static de.gematik.tim.test.glue.api.ActorMemoryKeys.MX_ID;
import static de.gematik.tim.test.glue.api.GeneralStepsGlue.checkResponseCode;
import static de.gematik.tim.test.glue.api.fhir.organisation.FhirOrgAdminGlue.findsAddressInHealthcareService;
import static de.gematik.tim.test.glue.api.message.DeleteMessageTask.deleteMessageWithId;
import static de.gematik.tim.test.glue.api.message.EditMessageTask.editMessage;
import static de.gematik.tim.test.glue.api.message.GetLastOwnMessageFromRoomQuestion.lastOwnMessage;
import static de.gematik.tim.test.glue.api.message.GetRoomMessagesQuestion.messagesInActiveRoom;
import static de.gematik.tim.test.glue.api.message.SendDirectMessageTask.sendDirectMessageTo;
import static de.gematik.tim.test.glue.api.message.SendMessageTask.sendMessage;
import static de.gematik.tim.test.glue.api.room.UseRoomAbility.addRoomToActor;
import static de.gematik.tim.test.glue.api.room.questions.GetRoomsQuestion.ownRooms;
import static de.gematik.tim.test.glue.api.utils.GlueUtils.filterForRoomWithSpecificMembers;
import static de.gematik.tim.test.glue.api.utils.GlueUtils.filterMessageForSenderAndText;
import static de.gematik.tim.test.glue.api.utils.GlueUtils.filterMessagesForSenderAndText;
import static java.util.Objects.requireNonNull;
import static net.serenitybdd.rest.SerenityRest.lastResponse;
import static net.serenitybdd.screenplay.actors.OnStage.setTheStage;
import static net.serenitybdd.screenplay.actors.OnStage.theActorCalled;
import static org.apache.commons.lang3.StringUtils.isNotBlank;
import static org.assertj.core.api.Assertions.assertThat;

import de.gematik.tim.test.glue.api.room.UseRoomAbility;
import de.gematik.tim.test.models.MessageDTO;
import de.gematik.tim.test.models.RoomDTO;
import io.cucumber.java.Before;
import io.cucumber.java.de.Dann;
import io.cucumber.java.de.Wenn;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import java.util.List;
import net.serenitybdd.screenplay.Actor;
import net.serenitybdd.screenplay.actors.Cast;

public class MessageControllerGlue {

  @Before
  public void setup() {
    setTheStage(Cast.ofStandardActors());
  }


  //<editor-fold desc="send message">
  @Wenn("{string} sendet die Nachricht {string} an den Raum {string}")
  @When("{string} sends message {string} in room {string}")
  public void sendsMessageInRoom(String actorName, String messageText, String roomName) {
    Actor actor = theActorCalled(actorName);
    actor.abilityTo(UseRoomAbility.class).setActive(roomName);
    actor.attemptsTo(sendMessage(messageText));
  }

  @When("{string} writes {string} directly {string}")
  @Wenn("{string} schreibt {string} direkt {string}")
  public void sendDirectMessage(String actorName, String userName, String message) {
    Actor actor1 = theActorCalled(actorName);
    String actor1Id = actor1.recall(MX_ID);
    Actor actor2 = theActorCalled(userName);
    String actor2Id = actor2.recall(MX_ID);
    actor1.attemptsTo(sendDirectMessageTo(actor2.recall(MX_ID), message));
    if (isNotBlank(actor1.recall(DIRECT_CHAT_NAME + actor2Id))
        || lastResponse().statusCode() != 200) {
      return;
    }
    List<String> membersIds = List.of(actor1Id, actor2Id);
    List<RoomDTO> rooms = theActorCalled(actorName).asksFor(ownRooms());
    RoomDTO room = filterForRoomWithSpecificMembers(rooms, membersIds);
    assertThat(room).isNotNull();
    actor1.remember(DIRECT_CHAT_NAME + actor2Id, room.getName());
    actor2.remember(DIRECT_CHAT_NAME + actor1Id, room.getName());
    addRoomToActor(room, actor1);
    addRoomToActor(room, actor2);
  }

  @When("{string} writes {string} via healthcare service {string} directly {string}")
  @Wenn("{string} schreibt {string} über den Healthcare-Service {string} direkt {string}")
  public void writsDirectlyToHealthcareService(String actorName, String userName, String hsName,
      String message) {
    findsAddressInHealthcareService(actorName, userName, hsName);
    sendDirectMessage(actorName, userName, message);
  }

  @Wenn("{string} versucht {string} direkt {string} zu schreiben")
  public void triesToWriteDirectly(String actorName, String userName, String message) {
    Actor actor1 = theActorCalled(actorName);
    Actor actor2 = theActorCalled(userName);
    actor1.attemptsTo(sendDirectMessageTo(actor2.recall(MX_ID), message));
    checkResponseCode(actorName, 403);
  }

  @Wenn("{string} ist nicht berechtigt {string} zu kontaktieren")
  public void notAuthorizedForCommunication(String actorName, String userName) {
    checkResponseCode(actorName, 403);
  }
  //</editor-fold>

  //<editor-fold desc="Receive messages">
  @Then("{listOfStrings} can see message {listOfStrings} from {string} in room {string}")
  @Then("{listOfStrings} can see messages {listOfStrings} from {string} in room {string}")
  @Dann("{listOfStrings} empfängt eine Nachricht {listOfStrings} von {string} im Raum {string}")
  @Dann("{listOfStrings} empfängt die Nachrichten {listOfStrings} von {string} im Raum {string}")
  @Dann("{listOfStrings} empfangen eine Nachricht {listOfStrings} von {string} im Raum {string}")
  @Dann("{listOfStrings} empfangen die Nachrichten {listOfStrings} von {string} im Raum {string}")
  public void canSeeMessagesInRoom(List<String> actorNames, List<String> messageTexts,
      String authorName, String roomName) {

    String authorId = theActorCalled(authorName).recall(MX_ID);

    actorNames.forEach(a -> {
      Actor actor = theActorCalled(a);
      actor.abilityTo(UseRoomAbility.class).setActive(roomName);
      List<MessageDTO> messages = actor.asksFor(messagesInActiveRoom());

      assertThat(messages)
          .filteredOn(msg -> authorId.equals(msg.getAuthor()))
          .extracting(MessageDTO::getBody)
          .containsExactlyInAnyOrder(messageTexts.toArray(new String[0]));
    });
  }

  @Dann("{string} empfängt eine Nachricht {string} von {string}")
  public void receiveMessageChat(String actorName, String textMessage, String senderName) {
    Actor actor = theActorCalled(actorName);
    String actorId = actor.recall(MX_ID);
    String senderId = theActorCalled(senderName).recall(MX_ID);
    List<String> membersIds = List.of(actorId, senderId);
    List<RoomDTO> rooms = actor.asksFor(ownRooms());
    RoomDTO room = requireNonNull(filterForRoomWithSpecificMembers(rooms, membersIds));

    actor.abilityTo(UseRoomAbility.class).setActive(room.getName());
    List<MessageDTO> messages = actor.asksFor(messagesInActiveRoom());
    List<MessageDTO> message = filterMessagesForSenderAndText(textMessage, senderName, messages);
    assertThat(message).hasSize(1);
  }

  @Then("{string} sees {int} messages in room {string}")
  public void seesMessagesInRoom(String actorName, int messagesCount, String roomName) {
    Actor actor = theActorCalled(actorName);
    actor.abilityTo(UseRoomAbility.class).setActive(roomName);
    List<MessageDTO> messages = actor.asksFor(messagesInActiveRoom());
    assertThat(messages).hasSize(messagesCount);
  }

  @Wenn("{string} could not see message {string} from {string} in chat with {string}")
  @Dann("{string} kann die Nachricht {string} von {string} im Chat mit {string} nicht sehen")
  public void canNotSeeMessage(String actorName, String message, String userName,
      String chatPartner) {
    String roomName = theActorCalled(actorName).recall(
        DIRECT_CHAT_NAME + theActorCalled(chatPartner).recall(MX_ID));
    cantFindMessageInRoom(actorName, message, userName, roomName);
  }

  @Then("{string} could not find message {string} from {string} in room {string}")
  @Dann("{string} kann die Nachricht {string} von {string} im Raum {string} nicht sehen")
  public void cantFindMessageInRoom(String actorName, String messageText, String userName,
      String roomName) {
    Actor actor = theActorCalled(actorName);
    actor.abilityTo(UseRoomAbility.class).setActive(roomName);
    List<MessageDTO> messages = actor.asksFor(messagesInActiveRoom());
    List<MessageDTO> filteredMessages = filterMessagesForSenderAndText(messageText, userName,
        messages);
    assertThat(filteredMessages).as("Expected that no message should be found").hasSize(0);
  }
  //</editor-fold>

  //<editor-fold desc="Edit message">
  @When("{string} edits her last sent message in room {string} to {string}")
  @Wenn("{string} ändert seine letzte Nachricht im Raum {string} in {string}")
  public void editsHerLastSentMessageTo(String actorName, String roomName, String messageText) {
    Actor actor = theActorCalled(actorName);
    actor.abilityTo(UseRoomAbility.class).setActive(roomName);
    MessageDTO message = actor.asksFor(lastOwnMessage());
    actor.attemptsTo(editMessage().withMessage(messageText).withMessageId(message.getMessageId()));
  }
  //</editor-fold>

  //<editor-fold desc="Delete Message">
  @When("{string} deletes her last sent message in room {string}")
  public void deletesHerLastSentMessageInRoom(String actorName, String roomName) {
    Actor actor = theActorCalled(actorName);
    actor.abilityTo(UseRoomAbility.class).setActive(roomName);
    MessageDTO message = actor.asksFor(lastOwnMessage());
    actor.attemptsTo(deleteMessageWithId(message.getMessageId()));
  }

  @When("{string} deletes his message {string} in chat with {string}")
  @Wenn("{string} löscht seine Nachricht {string} im Chat mit {string}")
  public void deleteMessageInChat(String actorName, String messageText, String userName) {
    String roomName = theActorCalled(actorName).recall(
        DIRECT_CHAT_NAME + theActorCalled(userName).recall(MX_ID));
    deleteMessageInRoom(actorName, messageText, roomName);
  }

  @When("{string} löscht seine Nachricht {string} im Raum {string}")
  public void deleteMessageInRoom(String actorName, String messageText, String roomName) {
    Actor actor = theActorCalled(actorName);
    actor.abilityTo(UseRoomAbility.class).setActive(roomName);
    List<MessageDTO> messages = actor.asksFor(messagesInActiveRoom());
    MessageDTO message = filterMessageForSenderAndText(messageText, actorName, messages);
    actor.attemptsTo(deleteMessageWithId(message.getMessageId()));
  }
  //</editor-fold>


}
