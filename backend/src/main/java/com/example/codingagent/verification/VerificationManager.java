package com.example.codingagent.verification;

import com.example.codingagent.agent.AgentState;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Verification coordinator delegating to the comprehensive evidence-based VerificationService.
 */
@Component
public class VerificationManager {

    private final VerificationService verificationService;

    @Autowired
    public VerificationManager(VerificationService verificationService) {
        this.verificationService = verificationService;
    }

    public VerificationResult verify(AgentState state) {
        return verificationService.verify(state);
    }

    public TaskResult generateTaskResult(AgentState state, VerificationResult verification) {
        return verificationService.generateTaskResult(state, verification);
    }
}
