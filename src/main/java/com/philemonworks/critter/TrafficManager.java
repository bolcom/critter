package com.philemonworks.critter;

import java.util.List;

import com.google.inject.Inject;
import com.philemonworks.critter.dao.RuleDao;
import com.philemonworks.critter.rule.Rule;
import com.philemonworks.critter.rule.RuleContext;
import com.sun.jersey.api.core.HttpContext;

public class TrafficManager {
    @Inject RuleDao ruleDao;
    
    public Rule detectRule(HttpContext context) {
        for (Rule each : this.ruleDao.getRules()) {
            RuleContext ruleContext = new RuleContext();
            ruleContext.httpContext = context;
            if (each.enabled && each.test(ruleContext))
                return each;
        }
        return null; // no matching rule
    }

    public void performRule(Rule aRule, RuleContext ruleContext) {
        aRule.perform(ruleContext);
    }

    public void addOrReplaceRule(Rule aRule) {
        aRule.ensureId();
        this.ruleDao.addOrReplaceRule(aRule);
    }

    public Rule getRule(String id) {
        return this.ruleDao.getRule(id);
    }

    public List<Rule> getAllRules() {
        return this.ruleDao.getRules();
    }

    public void deleteRule(String id) {
        this.ruleDao.deleteRule(id);
    }
}