package com.example.reservation;

public class ReservationService {

    private final RoomRepository roomRepository;
    private final ReservationRepository reservationRepository;
    private final ReservationEmailService emailService;
    private final ConfirmationCodeGenerator codeGenerator;
    private final EventPublisher eventPublisher;

    public ReservationService(RoomRepository roomRepository,
                              ReservationRepository reservationRepository,
                              ReservationEmailService emailService,
                              ConfirmationCodeGenerator codeGenerator,
                              EventPublisher eventPublisher) {
        this.roomRepository = roomRepository;
        this.reservationRepository = reservationRepository;
        this.emailService = emailService;
        this.codeGenerator = codeGenerator;
        this.eventPublisher = eventPublisher;
    }

    /**
     * Tworzy rezerwację sali konferencyjnej.
     * <p>
     * Kolejność operacji:
     * 1. Walidacja (sala istnieje, pojemność wystarczająca, termin wolny)
     * 2. Generowanie kodu potwierdzenia
     * 3. Zapis rezerwacji do bazy
     * 4. Wysyłka emaila z potwierdzeniem
     * 5. Publikacja zdarzenia
     */
    public Reservation createReservation(String roomId, String organizerEmail,
                                         TimeSlot timeSlot, int attendees) {
        // 1. Walidacja - sala
        Room room = roomRepository.findById(roomId)
                .orElseThrow(() -> new IllegalArgumentException("Sala nie istnieje: " + roomId));

        if (attendees > room.getCapacity()) {
            throw new IllegalArgumentException(
                    String.format("Sala %s ma pojemność %d, żądano %d miejsc",
                            room.getName(), room.getCapacity(), attendees));
        }

        // 2. Walidacja - dostępność terminu
        if (reservationRepository.existsByRoomIdAndTimeSlot(roomId, timeSlot)) {
            throw new IllegalStateException(
                    "Sala " + room.getName() + " jest już zarezerwowana w tym terminie");
        }

        // 3. Generuj kod potwierdzenia
        String confirmationCode = codeGenerator.generate();

        // 4. Utwórz i zapisz rezerwację
        Reservation reservation = new Reservation();
        reservation.setRoomId(roomId);
        reservation.setOrganizerEmail(organizerEmail);
        reservation.setTimeSlot(timeSlot);
        reservation.setAttendees(attendees);
        reservation.setConfirmationCode(confirmationCode);
        reservation.setStatus(ReservationStatus.CONFIRMED);

        Reservation saved = reservationRepository.save(reservation);

        // 5. Wyślij email
        String subject = "Potwierdzenie rezerwacji sali " + room.getName();
        String body = String.format(
                "Rezerwacja potwierdzona!\nSala: %s\nData: %s\nGodziny: %s - %s\nKod: %s",
                room.getName(),
                timeSlot.getDate(),
                timeSlot.getStartTime(),
                timeSlot.getEndTime(),
                confirmationCode
        );
        emailService.sendConfirmation(new ConfirmationEmail(organizerEmail, subject, body));

        // 6. Opublikuj zdarzenie
        eventPublisher.publish("RESERVATION_CREATED",
                "Rezerwacja " + confirmationCode + " dla sali " + room.getName());

        return saved;
    }
}
