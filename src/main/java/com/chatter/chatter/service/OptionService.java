package com.chatter.chatter.service;

import com.chatter.chatter.model.Option;
import com.chatter.chatter.model.PollMessage;
import com.chatter.chatter.repository.OptionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class OptionService {

    private final OptionRepository optionRepository;

    @Transactional
    public List<Option> createOptions(Iterable<String> titles, PollMessage pollMessage) {
        ArrayList<Option> options = new ArrayList<>();
        for (String title : titles) {
            Option option = Option.builder()
                    .title(title)
                    .pollMessage(pollMessage)
                    .build();
            options.add(option);
        }
        return options;
    }

    @Transactional
    public List<Option> createOptions(List<Option> options, PollMessage pollMessage) {
        ArrayList<Option> createdOptions = new ArrayList<>();
        for (Option o : options) {
            Option option = Option.builder()
                    .title(o.getTitle())
                    .pollMessage(pollMessage)
                    .build();
            createdOptions.add(option);
        }
        return createdOptions;
    }

    public List<Option> getOptionsWithoutVotes(String email, Iterable<Long> optionsIds) {
        return optionRepository.findOptionsWithoutVotes(email, optionsIds);
    }

}
