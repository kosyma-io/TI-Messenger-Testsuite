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

import static de.gematik.tim.test.glue.api.ActorMemoryKeys.DIRECT_CHAT_NAME;
import static de.gematik.tim.test.glue.api.ActorMemoryKeys.MX_ID;
import static de.gematik.tim.test.glue.api.GeneralStepsGlue.checkResponseCode;
import static de.gematik.tim.test.glue.api.fhir.organisation.FhirOrgAdminGlue.findsAddressInHealthcareService;
import static de.gematik.tim.test.glue.api.room.UseRoomAbility.addRoomToActor;
import static de.gematik.tim.test.glue.api.room.questions.GetRoomQuestion.ownRoom;
import static de.gematik.tim.test.glue.api.room.questions.GetRoomStatesQuestion.roomStates;
import static de.gematik.tim.test.glue.api.room.questions.GetRoomsQuestion.ownRooms;
import static de.gematik.tim.test.glue.api.room.questions.RoomIdForRoomNameQuestion.roomIdForRoomName;
import static de.gematik.tim.test.glue.api.room.tasks.CreateRoomTask.createRoom;
import static de.gematik.tim.test.glue.api.room.tasks.DenyRoomInvitationTask.denyInvitation;
import static de.gematik.tim.test.glue.api.room.tasks.ForgetRoomTask.forgetRoom;
import static de.gematik.tim.test.glue.api.room.tasks.InviteToRoomTask.invite;
import static de.gematik.tim.test.glue.api.room.tasks.JoinRoomTask.joinRoom;
import static de.gematik.tim.test.glue.api.room.tasks.LeaveRoomTask.leaveRoom;
import static de.gematik.tim.test.glue.api.utils.GlueUtils.filterForRoomWithSpecificMembers;
import static de.gematik.tim.test.glue.api.utils.GlueUtils.filterForRoomsWithSpecificMembers;
import static de.gematik.tim.test.models.RoomMembershipStateDTO.INVITE;
import static de.gematik.tim.test.models.RoomMembershipStateDTO.JOIN;
import static java.lang.String.format;
import static java.util.Objects.requireNonNull;
import static net.serenitybdd.screenplay.actors.OnStage.theActorCalled;
import static net.serenitybdd.screenplay.actors.OnStage.theActorInTheSpotlight;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.http.HttpStatus.CREATED;
import static org.springframework.http.HttpStatus.FORBIDDEN;
import static org.springframework.http.HttpStatus.NO_CONTENT;
import static org.springframework.http.HttpStatus.OK;
import static org.springframework.http.HttpStatus.UNAUTHORIZED;

import de.gematik.tim.test.glue.api.room.questions.RoomStates;
import de.gematik.tim.test.models.RoomDTO;
import de.gematik.tim.test.models.RoomMemberDTO;
import de.gematik.tim.test.models.RoomMembershipStateDTO;
import de.gematik.tim.test.models.RoomStateDTO;
import io.cucumber.java.de.Dann;
import io.cucumber.java.de.Und;
import io.cucumber.java.de.Wenn;
import io.cucumber.java.en.And;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import net.serenitybdd.screenplay.Actor;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
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
  }
  //</editor-fold>

  //<editor-fold desc="Invite to room">
  @When("{string} invites {listOfStrings} into room {string}")
  @Und("{string} lädt {listOfStrings} in Chat-Raum {string} ein")
  public void inviteUserToChatRoomSuccessfully(String actorName, List<String> inviteActors, String roomName) {
    inviteUserToChatRoom(actorName, inviteActors, roomName);
    checkResponseCode(actorName, OK.value());
  }

  @When("{string} tries to invites {listOfStrings} into room {string}")
  @Und("{string} versucht {listOfStrings} in Chat-Raum {string} einzuladen")
  public void triesToInviteUserToChatRoom(String actorName, List<String> inviteActors, String roomName) {
    inviteUserToChatRoom(actorName, inviteActors, roomName);
    checkResponseCode(actorName, FORBIDDEN.value());
  }

  private void inviteUserToChatRoom(String actorName, List<String> inviteActors, String roomName) {
    Actor actor = theActorCalled(actorName);
    List<String> inviteMxids = new ArrayList<>();
    inviteActors.forEach(e -> inviteMxids.add(theActorCalled(e).recall(MX_ID)));

    String roomId = actor.abilityTo(UseRoomAbility.class).getRoomIdByName(roomName);
    RoomDTO room = new RoomDTO().roomId(roomId).name(roomName);
    inviteActors.forEach(e -> addRoomToActor(room, theActorCalled(e)));

    actor.attemptsTo(invite(inviteMxids).toRoom(roomId));
  }

  @When("{string} invites {string} into room with {string}")
  @Und("{string} lädt {string} in Chat mit {string} ein")
  public void inviteUserToChatSuccessfully(String invitingActorName, String invitedActorName, String thirdActorName) {
    inviteUserToChatWith(invitingActorName, invitedActorName, thirdActorName);
    checkResponseCode(invitingActorName, OK.value());
  }

  @When("{string} tries to invite {string} into room with {string}")
  @Und("{string} versucht {string} in Chat mit {string} einzuladen")
  public void triesToInviteUserToChatWith(String invitingActorName, String invitedActorName, String thirdActorName) {
    inviteUserToChatWith(invitingActorName, invitedActorName, thirdActorName);
    checkResponseCode(invitingActorName, UNAUTHORIZED.value());
  }

  private void inviteUserToChatWith(String invitingActorName, String invitedActorName, String thirdActorName) {
    Actor invitingActor = theActorCalled(invitingActorName);
    String invitedMxid = theActorCalled(invitedActorName).recall(MX_ID);
    String thirdMxid = theActorCalled(thirdActorName).recall(MX_ID);

    String roomName = invitingActor.recall(DIRECT_CHAT_NAME + thirdMxid);
    String roomId = invitingActor.abilityTo(UseRoomAbility.class).getRoomIdByName(roomName);

    invitingActor.attemptsTo(invite(List.of(invitedMxid)).toRoom(roomId));
  }

  @Und("{string} lädt {string} über den HealthcareService {string} in den Chat-Raum {string} ein")
  public void invitesUserToRoomViaHealthcareService(String actorName, String username,
                                                    String hsName, String roomName) {
    findsAddressInHealthcareService(actorName, username, hsName);
    inviteUserToChatRoom(actorName, List.of(username), roomName);
  }

  @Then("{string} versucht die MXID {listOfStrings} in den Chat-Raum {string} einzuladen")
  public void inviteMxid(String actorName, List<String> mxids, String roomName) {
    Actor actor = theActorCalled(actorName);
    String roomId = actor.abilityTo(UseRoomAbility.class).getRoomIdByName(roomName);
    actor.attemptsTo(invite(mxids).toRoom(roomId));
    checkResponseCode(actorName, FORBIDDEN.value());
  }
  //</editor-fold>

  //<editor-fold desc="Join room">
  @When("{listOfStrings} receive invitation from {string}")
  @Wenn("{listOfStrings} erhält eine Einladung von {string}")
  @Wenn("{listOfStrings} erhalten eine Einladung von {string}")
  public void receiveInvitation(List<String> actorNames, String userName) {
    List<String> chatMembersIds = actorNames.stream()
        .map(name -> (String) theActorCalled(name).recall(MX_ID))
        .collect(Collectors.toList());
    String invitingActor = theActorCalled(userName).recall(MX_ID);
    chatMembersIds.add(invitingActor);
    String roomId = theActorCalled(userName).abilityTo(UseRoomAbility.class).getActive();
    for (String actorName : actorNames) {
      Actor actor1 = theActorCalled(actorName);
      String actor1Id = actor1.recall(MX_ID);

      RoomDTO room = theActorCalled(actorName).asksFor(ownRoom().withRoomId(roomId));
      RoomMemberDTO member = requireNonNull(room.getMembers()).stream()
          .filter(m -> requireNonNull(m.getMxid()).equals(actor1Id)).findFirst().orElseThrow();
      assertThat(member.getMembershipState()).isEqualTo(INVITE);
    }
  }

  @When("{string} accepts the invitation to room {string}")
  @Wenn("{string} bestätigt eine Einladung in Raum {string}")
  public void acceptsTheInvitationToRoom(String actorName, String roomName) {
    Actor actor = theActorCalled(actorName);
    String roomid = actor.asksFor(roomIdForRoomName(roomName));
    actor.attemptsTo(joinRoom().withRoomId(roomid));
    checkResponseCode(actorName, OK.value());
  }

  @Wenn("{listOfStrings} bestätigt eine Einladung von {string}")
  @Wenn("{listOfStrings} bestätigen eine Einladung von {string}")
  public void acceptChatInvitation(List<String> actorNames, String inviter) {
    for (String actorName : actorNames) {
      Actor actor1 = theActorCalled(actorName);
      String actor1Id = actor1.recall(MX_ID);
      Actor actor2 = theActorCalled(inviter);
      String actor2Id = actor2.recall(MX_ID);
      RoomDTO room = theActorCalled(actorName).asksFor(ownRoom().
          withMembers(List.of(actor1Id, actor2Id)));
      actor1.attemptsTo(joinRoom().withRoomId(room.getRoomId()));
      checkResponseCode(actorName, OK.value());
    }
  }

  @When("{string} deny room invitation to {string}")
  @Wenn("{string} lehnt eine Einladung für Raum {string} ab")
  public void denyRoomInvitationTo(String actorName, String roomName) {
    Actor actor = theActorCalled(actorName);
    String roomid = actor.asksFor(roomIdForRoomName(roomName));
    actor.attemptsTo(denyInvitation().toRoom(roomid));
    checkResponseCode(actorName, OK.value());
    actor.attemptsTo(forgetRoom());
    checkResponseCode(actorName, NO_CONTENT.value());
  }

  @When("{string} deny chat invitation with {string}")
  @Wenn("{string} lehnt eine Einladung zum Chat mit {string} ab")
  public void denyChatInvitationTo(String actorName, String userName) {
    Actor actor = theActorCalled(actorName);
    List<RoomDTO> rooms = actor.asksFor(ownRooms());
    RoomDTO room = requireNonNull(
        filterForRoomWithSpecificMembers(rooms,
            List.of(actor.recall(MX_ID), theActorCalled(userName).recall(MX_ID))));
    addRoomToActor(room, actor);
    leaveAndForgetRoom(actorName, actor);

  }
  //</editor-fold>

  //<editor-fold desc="Leave room">
  @When("{string} leaves chat with {string}")
  @Wenn("{string} verlässt Chat mit {string}")
  public void exitUserChat(String actorName, String userName) {
    Actor actor = theActorCalled(actorName);
    String actor2Id = theActorCalled(userName).recall(MX_ID);

    String roomName = actor.recall(DIRECT_CHAT_NAME + actor2Id);
    assertThat(roomName).as("%s have no direct chat with %s", actorName, userName).isNotBlank();
    actor.abilityTo(UseRoomAbility.class).setActive(roomName);
    leaveAndForgetRoom(actorName, actor);

    actor.forget(DIRECT_CHAT_NAME + actor2Id);
    List<RoomDTO> rooms = actor.asksFor(ownRooms());
    assertThat(rooms).extracting(RoomDTO::getName).doesNotContain(roomName);
  }


  @When("{string} leave room with name {string}")
  @Wenn("{string} verlässt Chat-Raum {string}")
  public void exitUserChatRoom(String actorName, String roomName) {
    Actor actor = theActorCalled(actorName);
    actor.abilityTo(UseRoomAbility.class).setActive(roomName);
    leaveAndForgetRoom(actorName, actor);
  }

  private static void leaveAndForgetRoom(String actorName, Actor actor) {
    actor.attemptsTo(leaveRoom());
    checkResponseCode(actorName, OK.value());
    actor.attemptsTo(forgetRoom());
    checkResponseCode(actorName, NO_CONTENT.value());
  }
  //</editor-fold>
  //<editor-fold desc="Delete room">

  @When("{string} deletes room {string}")
  public void deletesRoom(String actorName, String roomName) {
    Actor actor = theActorCalled(actorName);
    actor.abilityTo(UseRoomAbility.class).setActive(roomName);
    actor.attemptsTo(forgetRoom());
    checkResponseCode(actorName, NO_CONTENT.value());
  }
  //</editor-fold>
  //<editor-fold desc="RoomState">

  @Then("{string} prüft den Room State im Chat mit {string} auf {listOfStrings}")
  public void checkRoomStatesOfChat(String actorName, String userName, List<String> roomStates) {
    String roomName = theActorCalled(actorName).recall(DIRECT_CHAT_NAME + theActorCalled(userName).recall(MX_ID));
    checkRoomStatesOfRoom(actorName, roomName, roomStates);
  }

  @Then("{string} prüft den Room State im Raum {string} auf {listOfStrings}")
  public void checkRoomStatesOfRoom(String actorName, String roomName, List<String> requestedRoomStates) {
    Actor actor = theActorCalled(actorName);
    actor.abilityTo(UseRoomAbility.class).setActive(roomName);
    List<RoomStateDTO> roomStates = actor.asksFor(roomStates());
    Set<String> roomStatesNames = requestedRoomStates.stream().map(RoomStates::valueOf).map(RoomStates::getText).collect(Collectors.toSet());
    assertThat(roomStates).extracting(RoomStateDTO::getType).containsAll(roomStatesNames);
  }

  //</editor-fold>
  //<editor-fold desc="Conditions">

  @Then("the room {string} has {int} {} members")
  public void theRoomHasMembers(String roomName, int memberCount,
                                RoomMembershipStateDTO membershipState) {
    List<RoomDTO> rooms = theActorInTheSpotlight().asksFor(ownRooms());
    RoomDTO room = rooms.stream().filter(e -> roomName.equals(e.getName())).findAny().orElseThrow();
    assertThat(requireNonNull(room.getMembers()).stream()
        .filter(e -> membershipState.equals(e.getMembershipState()))).hasSize(memberCount);
  }

  @Then("the room {string} of {string} have {int} members")
  public void haveTheRoom(String roomName, String actorName, int memberCount) {
    List<RoomDTO> rooms = theActorCalled(actorName).asksFor(ownRooms());
    RoomDTO room = rooms.stream().filter(e -> roomName.equals(e.getName())).findAny().orElseThrow();
    assertThat(room.getMembers()).hasSize(memberCount);
  }

  @Then("{string} should have {int} rooms")
  public void shouldHaveRooms(String actorName, int roomCount) {
    Actor actor = theActorCalled(actorName);
    List<RoomDTO> rooms = actor.asksFor(ownRooms());
    assertThat(rooms).hasSize(roomCount);
  }

  @Then("{string} have joined room {string}")
  @Dann("{string} ist dem Raum {string} beigetreten")
  public void userInRoom(String actorName, String roomName) {
    Actor actor = theActorCalled(actorName);
    List<RoomDTO> rooms = actor.asksFor(ownRooms());
    assertThat(rooms).extracting(RoomDTO::getName).contains(roomName);
    List<RoomMemberDTO> members = rooms.stream()
        .flatMap(e -> requireNonNull(e.getMembers()).stream())
        .filter(m -> requireNonNull(m.getMxid()).equals(actor.recall(MX_ID)))
        .toList();
    members.forEach(m -> assertThat(m.getMembershipState()).isEqualTo(JOIN));
  }

  @And("{string} has not joined the room {string}")
  @Und("{string} ist dem Raum {string} nicht beigetreten")
  public void userNotInRoom(String actorName, String roomName) {
    Actor actor = theActorCalled(actorName);
    List<RoomDTO> rooms = actor.asksFor(ownRooms());
    assertThat(rooms).extracting(RoomDTO::getName).doesNotContain(roomName);
  }

  @And("{string} has not joined the room {string} [Retry {long} - {long}]")
  @Und("{string} ist dem Raum {string} nicht beigetreten [Retry {long} - {long}]")
  public void userNotInRoom(String actorName, String roomName, Long timeout, Long pollInterval) {
    Actor actor = theActorCalled(actorName);
    RoomDTO room = actor.asksFor(
        ownRoom()
            .withName(roomName)
            .withMemberHaveStatus(actor.recall(MX_ID), JOIN)
            .withCustomInterval(timeout, pollInterval));
    assertThat(room).isNull();
  }

  @Then("{string} did not joined chat with {string}")
  @Dann("{string} ist dem Chat mit {string} nicht beigetreten")
  @Dann("{string} erhält KEINE Einladung von {string}")
  public void userDidNotEnterChat(String actorName, String userName) {
    Actor actor = theActorCalled(actorName);
    List<RoomDTO> rooms = actor.asksFor(ownRooms());
    userDidNotEnterChat(userName, actor, rooms);
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
            .withMemberHaveStatus(actorMxid, JOIN)
            .withCustomInterval(timeout, pollInterval));
    assertThat(room).isNull();
  }

  private void userDidNotEnterChat(String userName, Actor actor, List<RoomDTO> rooms) {
    List<RoomDTO> filteredRooms = filterForRoomsWithSpecificMembers(rooms,
        List.of(actor.recall(MX_ID), theActorCalled(userName).recall(MX_ID)));
    assertThat(filteredRooms).as(format("%s have chat-room left with %s", actor, userName))
        .hasSize(0);
  }

  @Then("{string} receive invitation to room {string}")
  @Dann("{string} erhält eine Einladung in Raum {string}")
  public void receiveInvitationToRoom(String actorName, String roomName) {
    Actor actor = theActorCalled(actorName);
    RoomDTO room = actor.asksFor(ownRoom().withName(roomName));
    RoomMemberDTO member = room.getMembers().stream()
        .filter(m -> m.getMxid().equals(theActorCalled(actorName).recall(MX_ID)))
        .findAny()
        .orElseThrow();
    assertThat(member.getMembershipState()).isEqualTo(INVITE);
  }

  @And("{string} gets no invitation from {string} for the room {string}")
  @Und("{string} erhält KEINE Einladung von {string} für den Raum {string}")
  public void noInvitationForRoomReceived(String actorName, String clientName, String roomName) {
    List<RoomDTO> rooms = theActorCalled(actorName).asksFor(ownRooms());
    noInvitationForRoomReceived(actorName, clientName, roomName, rooms);
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

  private static void noInvitationForRoomReceived(String actorName, String clientName,
                                                  String roomName,
                                                  List<RoomDTO> rooms) {
    String inviterMxid = theActorCalled(clientName).recall(MX_ID);
    List<RoomDTO> filteredRooms = rooms.stream()
        .filter(r -> requireNonNull(r.getName()).equals(roomName))
        .filter(
            r -> requireNonNull(r.getMembers()).stream()
                .map(RoomMemberDTO::getMxid)
                .filter(Objects::nonNull)
                .toList().contains(inviterMxid))
        .toList();
    assertThat(filteredRooms)
        .as(format("%s did get invitation to room %s", actorName, roomName))
        .hasSize(0);
  }

  //</editor-fold>

}