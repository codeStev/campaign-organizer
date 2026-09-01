package com.campaignorganizer.characters.application.document.port.published;

/**
 * Published port: persists a document exactly as given (id and foreign keys
 * already resolved by the caller) instead of generating a new id — backup
 * import's counterpart to the normal create flow (ADR-0061).
 */
public interface DocumentImportPort {

    DocumentView importDocument(DocumentView view);
}
