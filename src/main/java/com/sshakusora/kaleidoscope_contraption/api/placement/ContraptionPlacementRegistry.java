package com.sshakusora.kaleidoscope_contraption.api.placement;

import net.minecraft.resources.ResourceLocation;

import java.util.*;

public final class ContraptionPlacementRegistry {
    public static final String PLACEMENT_RULE_TAG = "KCPlacementRule";

    private static final Map<ResourceLocation, List<RegisteredRule>> RULES = new HashMap<>();
    private static final Map<ResourceLocation, RegisteredRule> RULES_BY_ID = new HashMap<>();

    private ContraptionPlacementRegistry() {
    }

    public static synchronized void register(ResourceLocation pointType, ResourceLocation ruleId,
                                             ContraptionPlacementRule rule) {
        Objects.requireNonNull(pointType, "pointType");
        Objects.requireNonNull(ruleId, "ruleId");
        Objects.requireNonNull(rule, "rule");
        if (RULES_BY_ID.containsKey(ruleId)) {
            throw new IllegalArgumentException("Duplicate contraption placement rule id: " + ruleId);
        }
        RegisteredRule registeredRule = new RegisteredRule(pointType, ruleId, rule);
        RULES.computeIfAbsent(pointType, ignored -> new ArrayList<>()).add(registeredRule);
        RULES_BY_ID.put(ruleId, registeredRule);
    }

    public static synchronized boolean unregister(ResourceLocation ruleId) {
        RegisteredRule registeredRule = RULES_BY_ID.remove(ruleId);
        if (registeredRule == null) {
            return false;
        }
        List<RegisteredRule> rules = RULES.get(registeredRule.pointType());
        rules.remove(registeredRule);
        if (rules.isEmpty()) {
            RULES.remove(registeredRule.pointType());
        }
        return true;
    }

    public static Optional<RegisteredRule> find(ContraptionPlacementContext context) {
        List<RegisteredRule> rules;
        synchronized (ContraptionPlacementRegistry.class) {
            rules = List.copyOf(RULES.getOrDefault(context.pointType(), List.of()));
        }
        return rules.stream()
                .filter(rule -> rule.rule().matches(context))
                .max(Comparator.comparingInt(rule -> rule.rule().priority()));
    }

    public static synchronized Optional<RegisteredRule> findById(ResourceLocation ruleId) {
        return Optional.ofNullable(RULES_BY_ID.get(ruleId));
    }

    public record RegisteredRule(ResourceLocation pointType, ResourceLocation ruleId,
                                 ContraptionPlacementRule rule) {
    }
}
