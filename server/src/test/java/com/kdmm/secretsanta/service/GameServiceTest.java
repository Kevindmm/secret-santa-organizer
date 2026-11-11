package com.kdmm.secretsanta.service;

import com.kdmm.secretsanta.dto.GameRequest;
import com.kdmm.secretsanta.dto.GameSummaryDTO;
import com.kdmm.secretsanta.dto.ParticipantRequest;
import com.kdmm.secretsanta.exception.AlreadyAssignedException;
import com.kdmm.secretsanta.exception.DuplicateEmailException;
import com.kdmm.secretsanta.exception.GameNotFoundException;
import com.kdmm.secretsanta.exception.InsufficientParticipantsException;
import com.kdmm.secretsanta.exception.ParticipantNotFoundException;
import com.kdmm.secretsanta.model.Game;
import com.kdmm.secretsanta.model.Participant;
import com.kdmm.secretsanta.repository.GameRepository;
import com.kdmm.secretsanta.repository.ParticipantRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class GameServiceTest {

    private static final String GAME_ID = "ABC12345";
    private static final LocalDate EXCHANGE_DATE = LocalDate.of(2026, 12, 24);

    private GameRepository gameRepo;
    private ParticipantRepository participantRepo;
    private EmailService emailService;
    private GameService gameService;

    @BeforeEach
    void setUp() {
        gameRepo = mock(GameRepository.class);
        participantRepo = mock(ParticipantRepository.class);
        emailService = mock(EmailService.class);
        gameService = new GameService(gameRepo, participantRepo, emailService);
    }

    // ---------------------------------------------------------------
    // Sorteo: justo, sin autoasignación y sin repetidos
    // ---------------------------------------------------------------

    @RepeatedTest(25)
    @DisplayName("El sorteo no se autoasigna y cada participante recibe exactamente un regalo")
    void assignSecretSantas_createsFairDerangementWithoutDuplicates() {
        Game game = game(GAME_ID, false);
        List<Participant> participants = List.of(
                participant(1L, "ana@test.com"),
                participant(2L, "bruno@test.com"),
                participant(3L, "carla@test.com"),
                participant(4L, "diego@test.com"),
                participant(5L, "elena@test.com")
        );

        when(gameRepo.findById(GAME_ID)).thenReturn(Optional.of(game));
        when(participantRepo.findByGameId(GAME_ID)).thenReturn(participants);

        gameService.assignSecretSantas(GAME_ID);

        Set<String> receivers = new HashSet<>();
        for (Participant giver : participants) {
            String receiverEmail = giver.getAssignedToEmail();
            assertNotNull(receiverEmail, "Todo participante debe tener una asignación");
            assertNotEquals(giver.getEmail(), receiverEmail, "Nadie puede regalarse a sí mismo");
            assertTrue(receivers.add(receiverEmail), "Ningún receptor puede repetirse");
        }

        Set<String> expected = participants.stream()
                .map(Participant::getEmail)
                .collect(Collectors.toSet());
        assertEquals(expected, receivers, "Todos deben ser elegidos exactamente una vez");

        assertTrue(game.isAssignmentsDone());
        verify(gameRepo).save(game);
        verify(emailService, times(participants.size()))
                .sendAssignment(any(Participant.class), any(Participant.class), eq(game));
    }

    @RepeatedTest(10)
    @DisplayName("El sorteo funciona con el mínimo de 3 participantes")
    void assignSecretSantas_worksWithThreeParticipants() {
        Game game = game(GAME_ID, false);
        List<Participant> participants = List.of(
                participant(1L, "ana@test.com"),
                participant(2L, "bruno@test.com"),
                participant(3L, "carla@test.com")
        );

        when(gameRepo.findById(GAME_ID)).thenReturn(Optional.of(game));
        when(participantRepo.findByGameId(GAME_ID)).thenReturn(participants);

        gameService.assignSecretSantas(GAME_ID);

        Set<String> receivers = participants.stream()
                .map(Participant::getAssignedToEmail)
                .collect(Collectors.toSet());

        assertEquals(3, receivers.size());
        for (Participant giver : participants) {
            assertNotEquals(giver.getEmail(), giver.getAssignedToEmail());
        }
    }

    @Test
    @DisplayName("No se puede sortear dos veces el mismo juego")
    void assignSecretSantas_throwsWhenAlreadyAssigned() {
        Game game = game(GAME_ID, true);
        when(gameRepo.findById(GAME_ID)).thenReturn(Optional.of(game));

        assertThrows(AlreadyAssignedException.class,
                () -> gameService.assignSecretSantas(GAME_ID));

        verify(participantRepo, never()).findByGameId(any());
        verifyNoInteractions(emailService);
    }

    @Test
    @DisplayName("No se puede sortear con menos de 3 participantes")
    void assignSecretSantas_throwsWhenNotEnoughParticipants() {
        Game game = game(GAME_ID, false);
        when(gameRepo.findById(GAME_ID)).thenReturn(Optional.of(game));
        when(participantRepo.findByGameId(GAME_ID)).thenReturn(List.of(
                participant(1L, "ana@test.com"),
                participant(2L, "bruno@test.com")
        ));

        assertThrows(InsufficientParticipantsException.class,
                () -> gameService.assignSecretSantas(GAME_ID));

        verifyNoInteractions(emailService);
    }

    @Test
    @DisplayName("Sortear un juego inexistente lanza GameNotFoundException")
    void assignSecretSantas_throwsWhenGameDoesNotExist() {
        when(gameRepo.findById(GAME_ID)).thenReturn(Optional.empty());

        assertThrows(GameNotFoundException.class,
                () -> gameService.assignSecretSantas(GAME_ID));
    }

    // ---------------------------------------------------------------
    // Crear juego
    // ---------------------------------------------------------------

    @Test
    @DisplayName("Se puede crear un juego con presupuesto 0 € y se guarda correctamente")
    void createGame_persistsRequestData() {
        GameRequest request = new GameRequest("Navidad en casa", BigDecimal.ZERO, EXCHANGE_DATE);

        String gameId = gameService.createGame(request);

        ArgumentCaptor<Game> captor = ArgumentCaptor.forClass(Game.class);
        verify(gameRepo).save(captor.capture());

        Game saved = captor.getValue();
        assertNotNull(gameId);
        assertEquals(8, gameId.length());
        assertEquals(gameId, saved.getId());
        assertEquals("Navidad en casa", saved.getName());
        assertEquals(0, saved.getMaxPrice().compareTo(BigDecimal.ZERO));
        assertEquals(EXCHANGE_DATE, saved.getExchangeDate());
        assertFalse(saved.isAssignmentsDone());
    }

    // ---------------------------------------------------------------
    // Listar juegos
    // ---------------------------------------------------------------

    @Test
    @DisplayName("getAllGames devuelve el resumen de cada juego con su número de participantes")
    void getAllGames_returnsSummaryForEveryGame() {
        Game first = game("AAA11111", false);
        Game second = game("BBB22222", true);

        when(gameRepo.findAll()).thenReturn(List.of(first, second));
        when(participantRepo.countByGameId("AAA11111")).thenReturn(4L);
        when(participantRepo.countByGameId("BBB22222")).thenReturn(0L);

        List<GameSummaryDTO> summaries = gameService.getAllGames();

        assertEquals(2, summaries.size());

        GameSummaryDTO firstSummary = summaries.get(0);
        assertEquals("AAA11111", firstSummary.gameId());
        assertEquals("Navidad en casa", firstSummary.name());
        assertEquals(0, firstSummary.maxPrice().compareTo(new BigDecimal("50")));
        assertEquals(EXCHANGE_DATE, firstSummary.exchangeDate());
        assertEquals(4, firstSummary.participantCount());
        assertFalse(firstSummary.assignmentsDone());

        GameSummaryDTO secondSummary = summaries.get(1);
        assertEquals("BBB22222", secondSummary.gameId());
        assertEquals(0, secondSummary.participantCount());
        assertTrue(secondSummary.assignmentsDone());
    }

    // ---------------------------------------------------------------
    // Eliminar participante
    // ---------------------------------------------------------------

    @Test
    @DisplayName("Elimina un participante cuando el sorteo aún no se ha hecho")
    void deleteParticipant_removesParticipant() {
        Game game = game(GAME_ID, false);
        Participant participant = participant(7L, "ana@test.com");
        participant.setGame(game);

        when(gameRepo.findById(GAME_ID)).thenReturn(Optional.of(game));
        when(participantRepo.findById(7L)).thenReturn(Optional.of(participant));

        gameService.deleteParticipant(GAME_ID, 7L);

        verify(participantRepo).delete(participant);
    }

    @Test
    @DisplayName("No elimina participantes si el sorteo ya se hizo")
    void deleteParticipant_throwsWhenAssignmentsAreAlreadyDone() {
        Game game = game(GAME_ID, true);
        when(gameRepo.findById(GAME_ID)).thenReturn(Optional.of(game));

        assertThrows(AlreadyAssignedException.class,
                () -> gameService.deleteParticipant(GAME_ID, 7L));

        verify(participantRepo, never()).findById(anyLong());
        verify(participantRepo, never()).delete(any(Participant.class));
    }

    @Test
    @DisplayName("Eliminar un participante inexistente lanza ParticipantNotFoundException")
    void deleteParticipant_throwsWhenParticipantDoesNotExist() {
        Game game = game(GAME_ID, false);
        when(gameRepo.findById(GAME_ID)).thenReturn(Optional.of(game));
        when(participantRepo.findById(7L)).thenReturn(Optional.empty());

        assertThrows(ParticipantNotFoundException.class,
                () -> gameService.deleteParticipant(GAME_ID, 7L));

        verify(participantRepo, never()).delete(any(Participant.class));
    }

    @Test
    @DisplayName("No se puede eliminar un participante de otro juego")
    void deleteParticipant_throwsWhenParticipantBelongsToAnotherGame() {
        Game game = game(GAME_ID, false);
        Participant participant = participant(7L, "ana@test.com");
        participant.setGame(game("OTHER999", false));

        when(gameRepo.findById(GAME_ID)).thenReturn(Optional.of(game));
        when(participantRepo.findById(7L)).thenReturn(Optional.of(participant));

        assertThrows(ParticipantNotFoundException.class,
                () -> gameService.deleteParticipant(GAME_ID, 7L));

        verify(participantRepo, never()).delete(any(Participant.class));
    }

    @Test
    @DisplayName("Eliminar de un juego inexistente lanza GameNotFoundException")
    void deleteParticipant_throwsWhenGameDoesNotExist() {
        when(gameRepo.findById(GAME_ID)).thenReturn(Optional.empty());

        assertThrows(GameNotFoundException.class,
                () -> gameService.deleteParticipant(GAME_ID, 7L));

        verify(participantRepo, never()).findById(anyLong());
    }

    // ---------------------------------------------------------------
    // Añadir participante
    // ---------------------------------------------------------------

    @Test
    @DisplayName("Añadir un email ya registrado lanza DuplicateEmailException")
    void addParticipant_throwsOnDuplicateEmailIgnoringCase() {
        Game game = game(GAME_ID, false);
        when(gameRepo.findById(GAME_ID)).thenReturn(Optional.of(game));
        when(participantRepo.findByGameId(GAME_ID))
                .thenReturn(List.of(participant(1L, "Ana@Test.com")));

        ParticipantRequest request = new ParticipantRequest(
                "Ana", "ana@test.com", "Un libro");

        assertThrows(DuplicateEmailException.class,
                () -> gameService.addParticipant(GAME_ID, request));

        verify(participantRepo, never()).save(any(Participant.class));
    }

    @Test
    @DisplayName("Añadir un participante nuevo lo guarda asociado al juego")
    void addParticipant_savesParticipantWithGame() {
        Game game = game(GAME_ID, false);
        when(gameRepo.findById(GAME_ID)).thenReturn(Optional.of(game));
        when(participantRepo.findByGameId(GAME_ID)).thenReturn(List.of());

        ParticipantRequest request = new ParticipantRequest(
                "Ana", "ana@test.com", "Un libro");

        gameService.addParticipant(GAME_ID, request);

        ArgumentCaptor<Participant> captor = ArgumentCaptor.forClass(Participant.class);
        verify(participantRepo).save(captor.capture());

        Participant saved = captor.getValue();
        assertEquals("Ana", saved.getName());
        assertEquals("ana@test.com", saved.getEmail());
        assertEquals("Un libro", saved.getWishList());
        assertEquals(game, saved.getGame());
        assertFalse(saved.isEmailSent());
    }

    // ---------------------------------------------------------------
    // Helpers
    // ---------------------------------------------------------------

    private static Game game(String id, boolean assignmentsDone) {
        Game game = new Game(id);
        game.setName("Navidad en casa");
        game.setMaxPrice(new BigDecimal("50"));
        game.setExchangeDate(EXCHANGE_DATE);
        game.setAssignmentsDone(assignmentsDone);
        return game;
    }

    private static Participant participant(long id, String email) {
        Participant participant = new Participant();
        participant.setId(id);
        participant.setName("Participante " + id);
        participant.setEmail(email);
        return participant;
    }
}
