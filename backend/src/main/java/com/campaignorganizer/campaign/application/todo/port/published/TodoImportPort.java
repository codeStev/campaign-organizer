package com.campaignorganizer.campaign.application.todo.port.published;

/**
 * Published port: persists a todo exactly as given (id and foreign keys
 * already resolved by the caller) instead of generating a new id - backup
 * import's counterpart to the normal create flow (ADR-0061, mirrors
 * LooseThreadImportPort).
 */
public interface TodoImportPort {

    TodoView importTodo(TodoView view);
}
