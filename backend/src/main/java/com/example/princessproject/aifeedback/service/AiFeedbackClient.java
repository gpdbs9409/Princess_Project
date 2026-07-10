package com.example.princessproject.aifeedback.service;

/**
 * The only contract AI is allowed to fulfill: turn an already-computed context into
 * natural-language feedback. Implementations must never (re)calculate scores.
 */
public interface AiFeedbackClient {

    AiFeedbackResult generate(AiFeedbackContext context);

    String modelName();
}
