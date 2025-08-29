package com.chatter.chatter.unit.service;

import com.chatter.chatter.model.Option;
import com.chatter.chatter.model.PollMessage;
import com.chatter.chatter.repository.OptionRepository;
import com.chatter.chatter.service.OptionService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class OptionServiceTests {

    @Mock
    private OptionRepository optionRepository;

    @InjectMocks
    private OptionService optionService;

    @Test
    void createOptions_FromTitles_ShouldCreateOptions() {
        PollMessage pollMessage = new PollMessage();
        List<String> titles = List.of("Option 1", "Option 2", "Option 3");

        List<Option> result = optionService.createOptions(titles, pollMessage);

        assertEquals(3, result.size());
        assertEquals("Option 1", result.get(0).getTitle());
        assertEquals("Option 2", result.get(1).getTitle());
        assertEquals("Option 3", result.get(2).getTitle());
        assertEquals(pollMessage, result.get(0).getPollMessage());
        assertEquals(pollMessage, result.get(1).getPollMessage());
        assertEquals(pollMessage, result.get(2).getPollMessage());
    }

    @Test
    void createOptions_FromTitles_ShouldReturnEmptyList_WhenTitlesEmpty() {
        PollMessage pollMessage = new PollMessage();
        List<String> titles = List.of();

        List<Option> result = optionService.createOptions(titles, pollMessage);

        assertTrue(result.isEmpty());
    }

    @Test
    void createOptions_FromOptions_ShouldCreateOptions() {
        PollMessage pollMessage = new PollMessage();
        Option option1 = Option.builder().title("Option 1").build();
        Option option2 = Option.builder().title("Option 2").build();
        List<Option> options = List.of(option1, option2);

        List<Option> result = optionService.createOptions(options, pollMessage);

        assertEquals(2, result.size());
        assertEquals("Option 1", result.get(0).getTitle());
        assertEquals("Option 2", result.get(1).getTitle());
        assertEquals(pollMessage, result.get(0).getPollMessage());
        assertEquals(pollMessage, result.get(1).getPollMessage());
    }

    @Test
    void createOptions_FromOptions_ShouldReturnEmptyList_WhenOptionsEmpty() {
        PollMessage pollMessage = new PollMessage();
        List<Option> options = List.of();

        List<Option> result = optionService.createOptions(options, pollMessage);

        assertTrue(result.isEmpty());
    }

    @Test
    void getOptionsWithoutVotes_ShouldCallRepository() {
        List<Long> optionIds = List.of(1L, 2L, 3L);
        List<Option> expectedOptions = List.of(
                Option.builder().id(1L).title("Option 1").build(),
                Option.builder().id(2L).title("Option 2").build()
        );

        when(optionRepository.findOptionsWithoutVotes("user@example.com", optionIds))
                .thenReturn(expectedOptions);

        List<Option> result = optionService.getOptionsWithoutVotes("user@example.com", optionIds);

        assertEquals(expectedOptions, result);
        verify(optionRepository).findOptionsWithoutVotes("user@example.com", optionIds);
    }

    @Test
    void getOptionsWithoutVotes_ShouldReturnEmptyList_WhenRepositoryReturnsEmpty() {
        List<Long> optionIds = List.of(1L, 2L, 3L);

        when(optionRepository.findOptionsWithoutVotes("user@example.com", optionIds))
                .thenReturn(List.of());

        List<Option> result = optionService.getOptionsWithoutVotes("user@example.com", optionIds);

        assertTrue(result.isEmpty());
        verify(optionRepository).findOptionsWithoutVotes("user@example.com", optionIds);
    }
}