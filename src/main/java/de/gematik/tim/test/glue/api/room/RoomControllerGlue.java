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

package de.gematik.tim.test.glue.api.room;

import static com.jayway.jsonpath.JsonPath.parse;
import static de.gematik.tim.test.glue.api.ActorMemoryKeys.DIRECT_CHAT_NAME;
import static de.gematik.tim.test.glue.api.ActorMemoryKeys.MX_ID;
import static de.gematik.tim.test.glue.api.GeneralStepsGlue.checkResponseCode;
import static de.gematik.tim.test.glue.api.fhir.organisation.FhirOrgAdminGlue.findsAddressInHealthcareService;
import static de.gematik.tim.test.glue.api.room.UseRoomAbility.addRoomToActor;
import static de.gematik.tim.test.glue.api.room.questions.GetRoomQuestion.ownRoom;
import static de.gematik.tim.test.glue.api.room.questions.GetRoomStatesQuestion.roomStates;
import static de.gematik.tim.test.glue.api.room.questions.GetRoomsQuestion.ownRooms;
import static de.gematik.tim.test.glue.api.room.tasks.CreateRoomTask.createRoom;
import static de.gematik.tim.test.glue.api.room.tasks.ForgetRoomTask.forgetRoom;
import static de.gematik.tim.test.glue.api.room.tasks.InviteToRoomTask.invite;
import static de.gematik.tim.test.glue.api.room.tasks.JoinRoomTask.joinRoom;
import static de.gematik.tim.test.glue.api.room.tasks.LeaveRoomTask.leaveRoom;
import static de.gematik.tim.test.glue.api.utils.GlueUtils.checkRoomMembershipState;
import static de.gematik.tim.test.glue.api.utils.TestcasePropertiesManager.getAllActiveActorsByMxIds;
import static de.gematik.tim.test.glue.api.utils.TestcasePropertiesManager.getRoomByInternalName;
import static de.gematik.tim.test.models.RoomMembershipStateDTO.INVITE;
import static de.gematik.tim.test.models.RoomMembershipStateDTO.JOIN;
import static java.lang.String.format;
import static java.util.Objects.nonNull;
import static java.util.Objects.requireNonNull;
import static net.serenitybdd.screenplay.actors.OnStage.theActorCalled;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.http.HttpStatus.CREATED;
import static org.springframework.http.HttpStatus.FORBIDDEN;
import static org.springframework.http.HttpStatus.NO_CONTENT;
import static org.springframework.http.HttpStatus.OK;
import static org.springframework.http.HttpStatus.UNAUTHORIZED;

import de.gematik.tim.test.glue.api.exceptions.TestRunException;
import de.gematik.tim.test.glue.api.room.questions.RoomStates;
import de.gematik.tim.test.models.RoomDTO;
import de.gematik.tim.test.models.RoomMemberDTO;
import de.gematik.tim.test.models.RoomStateDTO;
import io.cucumber.java.de.Dann;
import io.cucumber.java.de.Und;
import io.cucumber.java.de.Wenn;
import io.cucumber.java.en.And;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import net.serenitybdd.screenplay.Actor;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

public class RoomControllerGlue {

  //<editor-fold desc="Create room">
  @When("{string} creates a room with name {string}")
  @Wenn("{string} erstellt einen Chat-Raum {string}")
  public void createRoomWithName(String actorName, String roomName) {
    Actor actor = theActorCalled(actorName);
    actor.attemptsTo(createRoom().withName(roomName));
    checkResponseCode(actorName, CREATED.value());
    checkRoomMembershipState();
  }
  //</editor-fold>

  //<editor-fold desc="Invite to room">
  @When("{string} invites {listOfStrings} into room {string}")
  @Und("{string} lädt {listOfStrings} in Chat-Raum {string} ein")
  public void inviteUserToChatRoomSuccessfully(String actorName, List<String> inviteActors, String roomName) {
    inviteUserToChatRoom(actorName, inviteActors, roomName);
    checkResponseCode(actorName, OK.value());
    checkRoomMembershipState();
  }

  @When("{string} tries to invites {listOfStrings} into room {string}")
  @Und("{string} versucht {listOfStrings} in Chat-Raum {string} einzuladen")
  public void triesToInviteUserToChatRoom(String actorName, List<String> inviteActors, String roomName) {
    inviteUserToChatRoom(actorName, inviteActors, roomName);
    checkResponseCode(actorName, FORBIDDEN.value());
  }

  private void inviteUserToChatRoom(String actorName, List<String> inviteActors, String roomName) {
    Actor actor = theActorCalled(actorName);
    List<String> inviteMxids = inviteActors.stream().map(e -> (String) theActorCalled(e).recall(MX_ID)).toList();

    actor.abilityTo(UseRoomAbility.class).setActive(roomName);
    RoomDTO room = actor.abilityTo(UseRoomAbility.class).getActiveValue();
    inviteActors.forEach(e -> addRoomToActor(roomName, room, theActorCalled(e)));

    actor.attemptsTo(invite(inviteMxids).toRoom(room.getRoomId()));
  }

  @When("{string} invites {string} into chat with {string}")
  @Und("{string} lädt {string} in Chat mit {string} ein")
  public void inviteUserToChatSuccessfully(String invitingActorName, String invitedActorName, String thirdActorName) {
    inviteUserToChatWith(invitingActorName, invitedActorName, thirdActorName);
    checkResponseCode(invitingActorName, OK.value());
  }

  @When("{string} tries to invite {string} into chat with {string}")
  @Und("{string} versucht {string} in Chat mit {string} einzuladen")
  public void triesToInviteUserToChatWith(String invitingActorName, String invitedActorName, String thirdActorName) {
    inviteUserToChatWith(invitingActorName, invitedActorName, thirdActorName);
    checkResponseCode(invitingActorName, UNAUTHORIZED.value());
  }

  private void inviteUserToChatWith(String invitingActorName, String invitedActorName, String thirdActorName) {
    Actor invitingActor = theActorCalled(invitingActorName);
    String invitedMxid = theActorCalled(invitedActorName).recall(MX_ID);
    String thirdMxid = theActorCalled(thirdActorName).recall(MX_ID);

    String roomId = invitingActor.abilityTo(UseRoomAbility.class).getRoomIdByName(DIRECT_CHAT_NAME + thirdMxid);

    invitingActor.attemptsTo(invite(List.of(invitedMxid)).toRoom(roomId));
  }

  @Und("{string} lädt {string} über den HealthcareService {string} in den Chat-Raum {string} ein")
  public void invitesUserToRoomViaHealthcareService(String actorName, String username,
      String hsName, String roomName) {
    findsAddressInHealthcareService(actorName, username, hsName);
    inviteUserToChatRoom(actorName, List.of(username), roomName);
    checkRoomMembershipState();
  }

  @Then("{string} versucht die MXID {listOfStrings} in den Chat-Raum {string} einzuladen")
  public void inviteMxid(String actorName, List<String> mxids, String roomName) {
    Actor actor = theActorCalled(actorName);
    String roomId = actor.abilityTo(UseRoomAbility.class).getRoomIdByName(roomName);
    actor.attemptsTo(invite(mxids).toRoom(roomId).actorCouldBeFound(false));
    checkResponseCode(actorName, FORBIDDEN.value());
  }
  //</editor-fold>

  //<editor-fold desc="Join room">
  @When("{listOfStrings} receive invitation from {string}")
  @Wenn("{listOfStrings} erhält eine Einladung von {string}")
  @Wenn("{listOfStrings} erhalten eine Einladung von {string}")
  public void receiveInvitation(List<String> actorNames, String userName) {
    String roomId = theActorCalled(userName).abilityTo(UseRoomAbility.class).getActive().getRoomId();
    actorNames.forEach(actorName -> {
      RoomDTO room = theActorCalled(actorName).asksFor(ownRoom()
          .withRoomId(roomId)
          .withMemberHasStatus(theActorCalled(actorName).recall(MX_ID), INVITE));
      checkRoomMembershipState(room);
    });
  }

  @When("{string} accepts the invitation to room {string}")
  @Wenn("{string} bestätigt eine Einladung in Raum {string}")
  public void acceptsTheInvitationToRoom(String actorName, String roomName) {
    Actor actor = theActorCalled(actorName);
    RoomDTO room = actor.asksFor(ownRoom()
        .withName(roomName)
        .withMemberHasStatus(actor.recall(MX_ID), INVITE));
    checkRoomMembershipState(room);
    actor.attemptsTo(joinRoom().withRoomId(room.getRoomId()));
    checkResponseCode(actorName, OK.value());
    checkRoomMembershipState();
  }

  @Wenn("{listOfStrings} bestätigt eine Einladung von {string}")
  @Wenn("{listOfStrings} bestätigen eine Einladung von {string}")
  public void acceptChatInvitation(List<String> actorNames, String inviter) {
    Actor invitingActor = theActorCalled(inviter);
    String invitingMxid = invitingActor.recall(MX_ID);
    for (String actorName : actorNames) {
      Actor invitedActor = theActorCalled(actorName);
      String invitedMxid = invitedActor.recall(MX_ID);
      RoomDTO room = theActorCalled(actorName).asksFor(ownRoom()
          .withMemberHasStatus(invitedMxid, INVITE)
          .withMembers(List.of(invitedMxid, invitingMxid)));
      checkRoomMembershipState(room);
      invitedActor.attemptsTo(joinRoom().withRoomId(room.getRoomId()));
      checkResponseCode(actorName, OK.value());
      checkRoomMembershipState();
    }
  }
  //</editor-fold>

  //<editor-fold desc="Leave & Delete room">
  @When("{string} denies invitation to room {string}")
  @Wenn("{string} lehnt eine Einladung für Raum {string} ab")
  public void denyRoomInvitationTo(String actorName, String roomName) {
    Actor actor = theActorCalled(actorName);
    RoomDTO room = actor.asksFor(ownRoom().withName(roomName).withMemberHasStatus(actor.recall(MX_ID), INVITE));
    checkRoomMembershipState(room);
    addRoomToActor(roomName, room, actor);
    leaveAndForgetRoom(actor, room);
  }

  @When("{string} deny chat invitation with {string}")
  @Wenn("{string} lehnt eine Einladung zum Chat mit {string} ab")
  public void denyChatInvitationTo(String actorName, String userName) {
    String roomId = theActorCalled(userName).abilityTo(UseRoomAbility.class).getActive().getRoomId();
    Actor actor = theActorCalled(actorName);
    RoomDTO room = actor.asksFor(ownRoom()
        .withRoomId(roomId)
        .withMemberHasStatus(actor.recall(MX_ID), INVITE));
    checkRoomMembershipState(room);
    addRoomToActor(room, actor);
    leaveAndForgetRoom(actor, room);
  }

  @When("{string} leaves chat with {string}")
  @Wenn("{string} verlässt Chat mit {string}")
  public void exitUserChat(String actorName, String userName) {
    Actor actor = theActorCalled(actorName);
    String actor2Id = theActorCalled(userName).recall(MX_ID);

    String roomName = DIRECT_CHAT_NAME + actor2Id;
    actor.abilityTo(UseRoomAbility.class).setActive(roomName);
    leaveAndForgetRoom(actor, actor.abilityTo(UseRoomAbility.class).getActive());

    List<RoomDTO> rooms = actor.asksFor(ownRooms());
    assertThat(rooms).extracting(RoomDTO::getName).doesNotContain(roomName);
  }

  //</editor-fold>
  //<editor-fold desc="Delete room">
  @When("{string} leave room with name {string}")
  @Wenn("{string} verlässt Chat-Raum {string}")
  public void exitUserChatRoom(String actorName, String roomName) {
    Actor actor = theActorCalled(actorName);
    actor.abilityTo(UseRoomAbility.class).setActive(roomName);
    leaveAndForgetRoom(actor, actor.abilityTo(UseRoomAbility.class).getActive());

    List<RoomDTO> rooms = actor.asksFor(ownRooms());
    assertThat(rooms).extracting(RoomDTO::getName).doesNotContain(roomName);
  }

  private void leaveAndForgetRoom(Actor actor, RoomDTO room) {
    actor.attemptsTo(leaveRoom());
    checkResponseCode(actor.getName(), OK.value());
    actor.attemptsTo(forgetRoom());
    checkResponseCode(actor.getName(), NO_CONTENT.value());
    Optional<RoomMemberDTO> otherMemberInRoom = requireNonNull(room.getMembers())
        .stream()
        .filter(m -> !requireNonNull(m.getMxid()).equals(actor.recall(MX_ID)))
        .findFirst();
    if (otherMemberInRoom.isPresent()) {
      List<Actor> otherActorsInRoom = getAllActiveActorsByMxIds(List.of(requireNonNull(otherMemberInRoom.get().getMxid())));
      if (otherActorsInRoom.isEmpty()) {
        throw new TestRunException(format("Mxid <%s> not known in room <%s>", otherMemberInRoom.get().getMxid(), room.getName()));
      }
      RoomDTO updatedRoom = otherActorsInRoom.get(0).asksFor(
          ownRoom()
              .withRoomId(room.getRoomId())
              .notHavingMember(actor.recall(MX_ID)));
      checkRoomMembershipState(updatedRoom);
    }
  }
  //</editor-fold>

  //<editor-fold desc="RoomState">
  @Then("{string} prüft den Room State im Chat mit {string} auf {listOfStrings}")
  public void checkRoomStatesOfChat(String actorName, String userName, List<String> roomStates) {
    String roomName = DIRECT_CHAT_NAME + theActorCalled(userName).recall(MX_ID);
    checkRoomStatesOfRoom(actorName, roomName, roomStates);
  }

  @Then("{string} prüft den Room State im Raum {string} auf {listOfStrings}")
  public void checkRoomStatesOfRoom(String actorName, String roomName, List<String> requestedRoomStates) {
    Actor actor = theActorCalled(actorName);
    actor.abilityTo(UseRoomAbility.class).setActive(roomName);
    List<RoomStateDTO> roomStates = actor.asksFor(roomStates());
    Set<RoomStates> roomsStatesToCheck = requestedRoomStates.stream().map(RoomStates::valueOf)
        .collect(Collectors.toSet());
    for (RoomStates s : roomsStatesToCheck) {
      assertThat(isRoomStateInList(s, roomStates)).as(
          format("Expected roomState %s could not be found in Room %s", s, roomName));
    }
    checkRoomMembershipState(actor, roomName);
  }

  public boolean isRoomStateInList(RoomStates roomState, List<RoomStateDTO> roomStates) {
    roomStates = roomStates.stream().filter(r -> r.getType().equals(roomState.getType())).toList();
    if (nonNull(roomState.getContent())) {
      roomStates = roomStates.stream()
          .filter(
              r -> parse(r.getContent()).read(roomState.getContent().jsonPath()).equals(roomState.getContent().value()))
          .toList();
    }
    return !roomStates.isEmpty();
  }
  //</editor-fold>

  //<editor-fold desc="Conditions">
  @Then("{string} have joined room {string}")
  @Dann("{string} ist dem Raum {string} beigetreten")
  public void userInRoom(String actorName, String roomName) {
    Actor actor = theActorCalled(actorName);
    RoomDTO room = actor.asksFor(ownRoom()
        .withName(roomName)
        .withMemberHasStatus(actor.recall(MX_ID), JOIN));
    assertThat(room).as(format("Actor %s have not joined room %s", actorName, roomName)).isNotNull();
    checkRoomMembershipState(room);
  }

  @And("{string} has not joined the room {string}")
  @Und("{string} ist dem Raum {string} nicht beigetreten")
  public void userNotInRoom(String actorName, String roomName) {
    userNotInRoom(actorName, roomName, null, null);
  }

  @And("{string} has not joined the room {string} [Retry {long} - {long}]")
  @Und("{string} ist dem Raum {string} nicht beigetreten [Retry {long} - {long}]")
  public void userNotInRoom(String actorName, String roomName, Long timeout, Long pollInterval) {
    Actor actor = theActorCalled(actorName);
    RoomDTO room = actor.asksFor(
        ownRoom()
            .withRoomId(getRoomByInternalName(roomName).getRoomId())
            .withMemberHasStatus(actor.recall(MX_ID), JOIN)
            .withCustomInterval(timeout, pollInterval));
    assertThat(room).as(
        format("Actor %s should not joined room %s but was found in status %s", actorName, roomName, JOIN)).isNull();
  }

  @Then("{string} did not joined chat with {string}")
  @Dann("{string} ist dem Chat mit {string} nicht beigetreten")
  @Dann("{string} erhält KEINE Einladung von {string}")
  public void userDidNotEnterChat(String actorName, String userName) {
    userDidNotEnterChat(actorName, userName, null, null);
  }

  @Then("{string} did not joined chat with {string} [Retry {long} - {long}]")
  @Dann("{string} ist dem Chat mit {string} nicht beigetreten [Retry {long} - {long}]")
  @Dann("{string} erhält KEINE Einladung von {string} [Retry {long} - {long}]")
  public void userDidNotEnterChat(String actorName, String userName, Long timeout,
      Long pollInterval) {
    Actor actor = theActorCalled(actorName);
    String actorMxid = actor.recall(MX_ID);
    String userMxid = theActorCalled(userName).recall(MX_ID);
    RoomDTO room = actor.asksFor(
        ownRoom()
            .withMembers(List.of(actorMxid, userMxid))
            .withMemberHasStatus(actorMxid, JOIN)
            .withCustomInterval(timeout, pollInterval));
    assertThat(room).isNull();
  }

  @Then("{string} receive invitation to room {string}")
  @Dann("{string} erhält eine Einladung in Raum {string}")
  public void receiveInvitationToRoom(String actorName, String roomName) {
    Actor actor = theActorCalled(actorName);
    RoomDTO room = actor.asksFor(ownRoom()
        .withName(roomName)
        .withMemberHasStatus(actor.recall(MX_ID), INVITE));
    assertThat(room).as(format("Actor %s has no invitation to room %s", actorName, roomName)).isNotNull();
    checkRoomMembershipState(room);
  }

  @And("{string} gets no invitation from {string} for the room {string}")
  @Und("{string} erhält KEINE Einladung von {string} für den Raum {string}")
  public void noInvitationForRoomReceived(String actorName, String clientName, String roomName) {
    noInvitationForRoomReceived(actorName, clientName, roomName, null, null);
  }

  @And("{string} gets no invitation from {string} for the room {string} [Retry {long} - {long}]")
  @Und("{string} erhält KEINE Einladung von {string} für den Raum {string} [Retry {long} - {long}]")
  public void noInvitationForRoomReceived(String actorName, String userName, String roomName,
      Long timeout, Long pollInterval) {
    Actor actor = theActorCalled(actorName);
    String actorMxid = actor.recall(MX_ID);
    String userMxid = theActorCalled(userName).recall(MX_ID);
    RoomDTO room = theActorCalled(actorName).asksFor(
        ownRoom()
            .withMembers(List.of(actorMxid, userMxid))
            .withName(roomName)
            .withCustomInterval(timeout, pollInterval));
    assertThat(room).isNull();
  }

  //</editor-fold>

}