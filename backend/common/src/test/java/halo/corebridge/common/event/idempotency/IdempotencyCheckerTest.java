package halo.corebridge.common.event.idempotency;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("IdempotencyChecker 테스트")
class IdempotencyCheckerTest {

    @Nested
    @DisplayName("isDuplicate() - 중복 이벤트 확인")
    class IsDuplicateTest {

        @Test
        @DisplayName("성공: 이미 처리된 이벤트는 true를 반환한다")
        void isDuplicate_processedEvent_returnsTrue() {
            // given
            ProcessedEventRepository repo = mock(ProcessedEventRepository.class);
            IdempotencyChecker checker = new IdempotencyChecker(Optional.of(repo));
            given(repo.existsByEventIdAndConsumerGroup("event-1", "group-1")).willReturn(true);

            // when
            boolean result = checker.isDuplicate("event-1", "group-1");

            // then
            assertThat(result).isTrue();
        }

        @Test
        @DisplayName("성공: 처리되지 않은 이벤트는 false를 반환한다")
        void isDuplicate_newEvent_returnsFalse() {
            // given
            ProcessedEventRepository repo = mock(ProcessedEventRepository.class);
            IdempotencyChecker checker = new IdempotencyChecker(Optional.of(repo));
            given(repo.existsByEventIdAndConsumerGroup("event-new", "group-1")).willReturn(false);

            // when
            boolean result = checker.isDuplicate("event-new", "group-1");

            // then
            assertThat(result).isFalse();
        }

        @Test
        @DisplayName("성공: Repository가 없으면 항상 false를 반환한다 (멱등성 체크 스킵)")
        void isDuplicate_noRepository_returnsFalse() {
            // given
            IdempotencyChecker checker = new IdempotencyChecker(Optional.empty());

            // when
            boolean result = checker.isDuplicate("event-1", "group-1");

            // then
            assertThat(result).isFalse();
        }

        @Test
        @DisplayName("성공: eventId가 null이면 false를 반환한다")
        void isDuplicate_nullEventId_returnsFalse() {
            // given
            IdempotencyChecker checker = new IdempotencyChecker(Optional.empty());

            // when
            boolean result = checker.isDuplicate(null, "group-1");

            // then
            assertThat(result).isFalse();
        }

        @Test
        @DisplayName("성공: eventId가 빈 문자열이면 false를 반환한다")
        void isDuplicate_blankEventId_returnsFalse() {
            // given
            IdempotencyChecker checker = new IdempotencyChecker(Optional.empty());

            // when
            boolean result = checker.isDuplicate("  ", "group-1");

            // then
            assertThat(result).isFalse();
        }
    }

    @Nested
    @DisplayName("markAsProcessed() - 처리 완료 기록")
    class MarkAsProcessedTest {

        @Test
        @DisplayName("성공: 이벤트 처리 완료를 기록한다")
        void markAsProcessed_savesEvent() {
            // given
            ProcessedEventRepository repo = mock(ProcessedEventRepository.class);
            IdempotencyChecker checker = new IdempotencyChecker(Optional.of(repo));

            // when
            checker.markAsProcessed("event-1", "group-1");

            // then
            verify(repo, times(1)).save(any(ProcessedEvent.class));
        }

        @Test
        @DisplayName("성공: 동시 처리로 인한 중복 저장 시 예외를 무시한다")
        void markAsProcessed_duplicateSave_ignoresException() {
            // given
            ProcessedEventRepository repo = mock(ProcessedEventRepository.class);
            IdempotencyChecker checker = new IdempotencyChecker(Optional.of(repo));
            given(repo.save(any())).willThrow(new DataIntegrityViolationException("duplicate"));

            // when — 예외가 발생하지 않아야 한다
            checker.markAsProcessed("event-1", "group-1");

            // then
            verify(repo, times(1)).save(any(ProcessedEvent.class));
        }

        @Test
        @DisplayName("성공: Repository가 없으면 아무것도 하지 않는다")
        void markAsProcessed_noRepository_doesNothing() {
            // given
            IdempotencyChecker checker = new IdempotencyChecker(Optional.empty());

            // when
            checker.markAsProcessed("event-1", "group-1");

            // then — 예외 없이 정상 종료
        }

        @Test
        @DisplayName("성공: eventId가 null이면 저장하지 않는다")
        void markAsProcessed_nullEventId_doesNotSave() {
            // given
            ProcessedEventRepository repo = mock(ProcessedEventRepository.class);
            IdempotencyChecker checker = new IdempotencyChecker(Optional.of(repo));

            // when
            checker.markAsProcessed(null, "group-1");

            // then
            verify(repo, never()).save(any());
        }

        @Test
        @DisplayName("성공: eventId가 빈 문자열이면 저장하지 않는다")
        void markAsProcessed_blankEventId_doesNotSave() {
            // given
            ProcessedEventRepository repo = mock(ProcessedEventRepository.class);
            IdempotencyChecker checker = new IdempotencyChecker(Optional.of(repo));

            // when
            checker.markAsProcessed("", "group-1");

            // then
            verify(repo, never()).save(any());
        }
    }
}
