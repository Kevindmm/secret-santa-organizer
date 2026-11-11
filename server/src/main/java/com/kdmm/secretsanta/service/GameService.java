package com.kdmm.secretsanta.service;

import com.kdmm.secretsanta.dto.GameRequest;
import com.kdmm.secretsanta.dto.GameSummaryDTO;
import com.kdmm.secretsanta.dto.ParticipantDTO;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.util.*;

@Service
public class GameService {

    private static final Logger log = LoggerFactory.getLogger(GameService.class);

    private final GameRepository gameRepo;
    private final ParticipantRepository participantRepo;
    private final EmailService emailService;
    private final SecureRandom random = new SecureRandom();

    public GameService(GameRepository gameRepo,
                       ParticipantRepository participantRepo,
                       EmailService emailService) {
        this.gameRepo = gameRepo;
        this.participantRepo = participantRepo;
        this.emailService = emailService;
    }

    @Transactional
    public String createGame(GameRequest request) {
        log.info("Creating new Secret Santa game: {}", request.name());

        String gameId = generateGameCode();
        Game game = new Game();
        game.setId(gameId);
        game.setName(request.name());
        game.setMaxPrice(request.maxPrice());
        game.setExchangeDate(request.exchangeDate());
        game.setAssignmentsDone(false);

        gameRepo.save(game);
        log.debug("Game created with ID: {}", gameId);

        return gameId;
    }

    @Transactional
    public void addParticipant(String gameId, ParticipantRequest request) {
        log.info("Adding participant {} to game {}", request.email(), gameId);

        Game game = gameRepo.findById(gameId)
                .orElseThrow(() -> new GameNotFoundException(gameId));

        boolean emailExists = participantRepo.findByGameId(gameId).stream()
                .anyMatch(p -> p.getEmail().equalsIgnoreCase(request.email()));

        if (emailExists) {
            throw new DuplicateEmailException(request.email(), gameId);
        }

        Participant participant = new Participant();
        participant.setName(request.name());
        participant.setEmail(request.email());
        participant.setWishList(request.wishList());
        participant.setGame(game);
        participant.setEmailSent(false);

        participantRepo.save(participant);
        log.debug("Participant added: {} to game {}", request.email(), gameId);
    }

    @Transactional
    public void assignSecretSantas(String gameId) {
        log.info("Assigning Secret Santas for game: {}", gameId);

        Game game = gameRepo.findById(gameId)
                .orElseThrow(() -> new GameNotFoundException(gameId));

        if (game.isAssignmentsDone()) {
            throw new AlreadyAssignedException(gameId);
        }

        List<Participant> participants = participantRepo.findByGameId(gameId);
        if (participants.size() < 3) {
            throw new InsufficientParticipantsException(participants.size(), 3);
        }

        List<Participant> receivers = createValidReceiverAssignment(participants);

        executeSecretSantaAssignments(participants, receivers, game);

        game.setAssignmentsDone(true);
        gameRepo.save(game);

        log.info("Secret Santas assigned for game: {}. Emails sent to {} participants",
                gameId, participants.size());
    }

    /**
     * Builds a receiver list that is a cyclic permutation of the givers.
     *
     * <p>Uses the Sattolo algorithm: a uniformly random cyclic permutation.
     * By construction every participant moves to a different position, so:
     * <ul>
     *   <li>nobody is assigned to themselves (no self-gifting), and</li>
     *   <li>each participant appears exactly once as a receiver (no duplicates).</li>
     * </ul>
     * Unlike the previous retry-based shuffle, it always succeeds on the first
     * attempt (for 2+ participants) and does not bias the result.</p>
     */
    private List<Participant> createValidReceiverAssignment(List<Participant> participants) {
        List<Participant> receivers = new ArrayList<>(participants);

        // Sattolo's algorithm: j is drawn from [0, i) so index i never keeps its value.
        for (int i = receivers.size() - 1; i > 0; i--) {
            int j = random.nextInt(i);
            Collections.swap(receivers, i, j);
        }

        return receivers;
    }


     // Executes the actual Secret Santa assignments and sends emails
    private void executeSecretSantaAssignments(List<Participant> givers,
                                               List<Participant> receivers,
                                               Game game) {
        for (int i = 0; i < givers.size(); i++) {
            Participant giver = givers.get(i);
            Participant receiver = receivers.get(i);

            giver.setAssignedToEmail(receiver.getEmail());
            participantRepo.save(giver);

            emailService.sendAssignment(giver, receiver, game);
        }
    }

    @Transactional(readOnly = true)
    public List<GameSummaryDTO> getAllGames() {
        return gameRepo.findAll().stream()
                .map(game -> new GameSummaryDTO(
                        game.getId(),
                        game.getName(),
                        game.getMaxPrice(),
                        game.getExchangeDate(),
                        (int) participantRepo.countByGameId(game.getId()),
                        game.isAssignmentsDone()))
                .toList();
    }

    @Transactional
    public void deleteParticipant(String gameId, Long participantId) {
        log.info("Deleting participant {} from game {}", participantId, gameId);

        Game game = gameRepo.findById(gameId)
                .orElseThrow(() -> new GameNotFoundException(gameId));

        // Once the draw is done, the assignment pairs must stay intact.
        if (game.isAssignmentsDone()) {
            throw new AlreadyAssignedException(gameId);
        }

        Participant participant = participantRepo.findById(participantId)
                .orElseThrow(() -> new ParticipantNotFoundException(participantId));

        // Prevent deleting a participant that belongs to a different game.
        if (participant.getGame() == null || !gameId.equals(participant.getGame().getId())) {
            throw new ParticipantNotFoundException(participantId);
        }

        participantRepo.delete(participant);
        log.debug("Participant {} deleted from game {}", participantId, gameId);
    }

    public List<ParticipantDTO> getParticipants(String gameCode) {
        gameRepo.findById(gameCode.toUpperCase())
                .orElseThrow(() -> new GameNotFoundException(gameCode));

        List<Participant> participants = participantRepo.findByGameId(gameCode);

        return participants.stream()
                .map(p -> new ParticipantDTO(
                        p.getId(),
                        p.getName(),
                        p.getEmail(),
                        p.getAssignedToEmail() != null
                ))
                .toList();
    }

    private String generateGameCode() {
        String code = UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        log.trace("Generated game code: {}", code);
        return code;
    }
}