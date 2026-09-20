package com.example.reservation;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ReservationService - testy jednostkowe")
class ReservationServiceTest {

    @Mock
    private RoomRepository roomRepository;
    @Mock
    private ReservationRepository reservationRepository;
    @Mock
    private ReservationEmailService reservationEmailService;
    @Mock
    private ConfirmationCodeGenerator confirmationCodeGenerator;
    @Mock
    private EventPublisher eventPublisher;
    @InjectMocks
    private ReservationService reservationService;

    @Test
    @DisplayName("Powinien rzucić wyjątek gdy sala nie istnieje")
    void shouldThrowExceptionWhenRoomDoesNotExist() {
        //Arrange
        String nonExistingRoomId = "nonExistingRoomId";

        //Act
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> reservationService.createReservation(
                        nonExistingRoomId,
                        "organizer@example.com",
                        null,
                        10
                )
        );
        //Asert
        assertEquals("Sala nie istnieje: " + nonExistingRoomId, exception.getMessage());
    }

    @Test
    @DisplayName("Powinien rzucić wyjątek gdy za dużo uczestników")
    void shouldThrowExceptionWhenTooManyAttendees() {
        //Arrange
        String roomId = "room-1";
        Room room = new Room("room-1", "sala-wytrzezwien", 10, false);
        int requestedAttendees = 15;
        when(roomRepository.findById(roomId))
                .thenReturn(Optional.of(room));

        //Act
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> reservationService.createReservation(
                        roomId,
                        "organizer@example.com",
                        null,
                        requestedAttendees
                )
        );
        //Assert
        String expectedMessage = String.format("Sala %s ma pojemność %d, żądano %d miejsc",
                room.getName(), room.getCapacity(), requestedAttendees);
        assertEquals(expectedMessage, exception.getMessage());

    }

    @Test
    @DisplayName("Powinien rzucić wyjątek gdy termin zajęty")
    void shouldThrowExceptionWhenTimeSlotIsOccupied() {
        // Arrange
        String roomId = "room-1";
        Room room = new Room(roomId, "sala-wytrzezwien", 10, false);
        TimeSlot timeSlot = new TimeSlot(
                LocalDate.now(),
                LocalTime.of(10, 0),
                LocalTime.of(11, 0)
        );

        when(roomRepository.findById(roomId)).thenReturn(Optional.of(room));
        when(reservationRepository.existsByRoomIdAndTimeSlot(roomId, timeSlot))
                .thenReturn(true);

        // Act
        IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                () -> reservationService.createReservation(
                        roomId,
                        "organizer@example.com",
                        timeSlot,
                        5
                )
        );

        //Assert
        assertEquals("Sala " + room.getName() + " jest już zarezerwowana w tym terminie", exception.getMessage());
    }

    @Test
    @DisplayName("Powinien zapisać rezerwację z poprawnymi danymi")
    void shouldSaveReservationWithCorrectData() {
        // Arrange
        String roomId = "room-1";
        Room room = new Room(roomId, "sala-wytrzezwien", 10, false);
        TimeSlot timeSlot = new TimeSlot(LocalDate.now(), LocalTime.of(10, 0), LocalTime.of(11, 0));
        String organizerEmail = "organizer@example.com";
        String expectedCode = "RES-123";

        when(roomRepository.findById(roomId)).thenReturn(Optional.of(room));
        when(reservationRepository.existsByRoomIdAndTimeSlot(roomId, timeSlot)).thenReturn(false);
        when(confirmationCodeGenerator.generate()).thenReturn(expectedCode);

        // Act
        reservationService.createReservation(roomId, organizerEmail, timeSlot, 5);

        // Assert
        ArgumentCaptor<Reservation> reservationCaptor = ArgumentCaptor.forClass(Reservation.class);

        verify(reservationRepository).save(reservationCaptor.capture());
        Reservation capturedReservation = reservationCaptor.getValue();

        assertAll("Asercje danych rezerwacji",
                () -> assertEquals(roomId, capturedReservation.getRoomId()),
                () -> assertEquals(timeSlot, capturedReservation.getTimeSlot()),
                () -> assertEquals(organizerEmail, capturedReservation.getOrganizerEmail()),
                () -> assertEquals(expectedCode, capturedReservation.getConfirmationCode()),
                () -> assertEquals(ReservationStatus.CONFIRMED, capturedReservation.getStatus())
        );
    }

    @Test
    @DisplayName("Powinien wysłać email z poprawną treścią")
    void shouldSendEmailWithCorrectContent() {
        // Arrange
        String roomId = "room-1";
        Room room = new Room(roomId, "sala-wytrzezwien", 10, false);
        TimeSlot timeSlot = new TimeSlot(LocalDate.now(), LocalTime.of(10, 0), LocalTime.of(11, 0));
        String organizerEmail = "organizer@example.com";
        String expectedCode = "RES-123";

        when(roomRepository.findById(roomId)).thenReturn(Optional.of(room));
        when(reservationRepository.existsByRoomIdAndTimeSlot(roomId, timeSlot)).thenReturn(false);
        when(confirmationCodeGenerator.generate()).thenReturn(expectedCode);

        // Act
        reservationService.createReservation(roomId, organizerEmail, timeSlot, 5);

        // Assert
        ArgumentCaptor<ConfirmationEmail> emailCaptor = ArgumentCaptor.forClass(ConfirmationEmail.class);

        verify(reservationEmailService).sendConfirmation(emailCaptor.capture());
        ConfirmationEmail capturedEmail = emailCaptor.getValue();

        String expectedSubject = capturedEmail.getSubject();
        String expectedBody = String.format(
                "Rezerwacja potwierdzona!\nSala: %s\nData: %s\nGodziny: %s - %s\nKod: %s",
                room.getName(),
                timeSlot.getDate(),
                timeSlot.getStartTime(),
                timeSlot.getEndTime(),
                expectedCode);

        assertAll("Asercja treści",
                () -> assertEquals(organizerEmail, capturedEmail.getRecipientEmail()),
                () -> assertEquals(expectedBody, capturedEmail.getBody()),
                () -> assertEquals(expectedSubject, capturedEmail.getSubject())
        );

    }

    @Test
    @DisplayName("Powinien wykonać operacje w odpowiedniej kolejności")
    void shouldExecuteOperationsInCorrectOrder() {
        // Arrange
        String roomId = "room-1";
        Room room = new Room(roomId, "sala-wytrzezwien", 10, false);
        TimeSlot timeSlot = new TimeSlot(LocalDate.now(), LocalTime.of(10, 0), LocalTime.of(11, 0));
        String organizerEmail = "organizer@example.com";
        String expectedCode = "RES-123";

        when(roomRepository.findById(roomId)).thenReturn(Optional.of(room));
        when(reservationRepository.existsByRoomIdAndTimeSlot(roomId, timeSlot)).thenReturn(false);
        when(confirmationCodeGenerator.generate()).thenReturn(expectedCode);

        // Act
        reservationService.createReservation(roomId, organizerEmail, timeSlot, 5);

        // Assert
        InOrder inOrder = inOrder(
                roomRepository,
                reservationRepository,
                confirmationCodeGenerator,
                reservationEmailService,
                eventPublisher
        );

        inOrder.verify(roomRepository).findById(roomId);
        inOrder.verify(reservationRepository).existsByRoomIdAndTimeSlot(roomId, timeSlot);
        inOrder.verify(confirmationCodeGenerator).generate();
        inOrder.verify(reservationRepository).save(any(Reservation.class));
        inOrder.verify(reservationEmailService).sendConfirmation(any(ConfirmationEmail.class));
        inOrder.verify(eventPublisher).publish(eq("RESERVATION_CREATED"), anyString());
    }

    @Test
    @DisplayName("Powinien generować unikalne kody dla kolejnych rezerwacji")
    void shouldGenerateUniqueCodesForConsecutiveReservations() {
        // Arrange
        String roomId = "room-1";
        Room room = new Room(roomId, "sala-wytrzezwien", 10, false);
        TimeSlot timeSlot1 = new TimeSlot(LocalDate.now(), LocalTime.of(10, 0), LocalTime.of(11, 0));
        TimeSlot timeSlot2 = new TimeSlot(LocalDate.now(), LocalTime.of(12, 0), LocalTime.of(13, 0));

        when(roomRepository.findById(roomId)).thenReturn(Optional.of(room));
        when(reservationRepository.existsByRoomIdAndTimeSlot(eq(roomId), any(TimeSlot.class)))
                .thenReturn(false);
        when(reservationRepository.save(any(Reservation.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        AtomicInteger counter = new AtomicInteger(1);
        when(confirmationCodeGenerator.generate())
                .thenAnswer(invocation -> "RES-" + counter.getAndIncrement());

        // Act
        Reservation reservation1 = reservationService.createReservation(roomId, "organizer1@example.com", timeSlot1, 5);
        Reservation reservation2 = reservationService.createReservation(roomId, "organizer2@example.com", timeSlot2, 5);

        // Assert
        String code1 = reservation1.getConfirmationCode();
        String code2 = reservation2.getConfirmationCode();

        assertNotEquals(code1, code2);
        assertEquals("RES-1", code1);
        assertEquals("RES-2", code2);
    }

}

