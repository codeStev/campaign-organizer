package com.campaignorganizer.campaign.application.todo.service;

import com.campaignorganizer.campaign.application.todo.port.published.TodoView;
import com.campaignorganizer.campaign.domain.todo.Todo;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface TodoViewMapper {

    TodoView toView(Todo todo);
}
