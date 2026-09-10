package com.guoyu.platform.competition.exam.realtime;

public final class FormalExamRealtimeTypes {

  public static final String SESSION_STATUS_CHANGED = "session.status.changed";
  public static final String CANDIDATE_STATUS_CHANGED = "candidate.status.changed";
  public static final String CANDIDATE_PROGRESS_CHANGED = "candidate.progress.changed";
  public static final String ACCESS_SESSION_KICKED = "access-session.kicked";
  public static final String FORCE_SUBMIT_REQUESTED = "candidate.force-submit.requested";
  public static final String FORCE_SUBMIT_COMPLETED = "candidate.force-submit.completed";
  public static final String SNAPSHOT_RELOAD_REQUIRED = "snapshot.reload.required";
  public static final String COMMAND_ACK = "command.ack";
  public static final String APPLICATION_HEARTBEAT = "application.heartbeat";
  public static final String CANDIDATE_PRESENCE_CHANGED = "candidate.presence.changed";
  public static final String CONSOLE_SUBSCRIBE_PAGE = "console.subscribe-page";

  public static final String TARGET_SESSION = "examSession";
  public static final String TARGET_ACCESS = "accessSession";
  public static final String TARGET_CONSOLE = "console";

  public static final String CHANNEL = "formal-exam:realtime";
  public static final String TICKET_KEY_PREFIX = "formal-exam:ws-ticket:";
  public static final String CONSOLE_TICKET_KEY_PREFIX = "formal-exam:console-ticket:";
  public static final String PRESENCE_KEY_PREFIX = "formal-exam:presence:";

  private FormalExamRealtimeTypes() {
  }
}
