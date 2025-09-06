package com.example.wayneaiagent.agent;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.extern.slf4j.Slf4j;

/**
 * ReAct (Reasoning and Acting)
 */
@EqualsAndHashCode(callSuper = true)
@Data
@Slf4j
public abstract class ReActAgent extends BaseAgent {

    /**
     * think state and act
     *
     * @return true:next step，false:no act
     */
    public abstract boolean think();

    /**
     * acting
     *
     * @return
     */
    public abstract String act();


    @Override
    public String step() {
        try {
            boolean shouldAct = think();
            if (!shouldAct) {
                return "completed - no act";
            }
            return act();
        } catch (Exception e) {
            e.printStackTrace();
            return "fail step acting ：" + e.getMessage();
        }
    }

}