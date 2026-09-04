import { useEffect, useState } from "react";
import { fetchMeetingRecordingBlob } from "../api/meetingRecordings";

export function RecordingPlayer({ meetingId, recordingId }: { meetingId: string; recordingId: string }) {
  const [src, setSrc] = useState<string | null>(null);

  useEffect(() => {
    let objectUrl: string | null = null;
    let cancelled = false;
    fetchMeetingRecordingBlob(meetingId, recordingId).then((blob) => {
      if (cancelled) return;
      objectUrl = URL.createObjectURL(blob);
      setSrc(objectUrl);
    });
    return () => {
      cancelled = true;
      if (objectUrl) URL.revokeObjectURL(objectUrl);
    };
  }, [meetingId, recordingId]);

  if (!src) return <p className="table-hint">Loading audio...</p>;
  return <audio controls src={src} className="recording-player" />;
}
