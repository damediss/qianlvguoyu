package com.guoyu.platform.competition.player.sms;

import com.guoyu.platform.competition.enums.CompetitionPlayerSmsPurpose;

/** Plaintext mobile and code exist only in memory while a provider adapter sends the message. */
public record CompetitionSmsMessage(
    String mobile,
    CompetitionPlayerSmsPurpose purpose,
    String code) {
}
