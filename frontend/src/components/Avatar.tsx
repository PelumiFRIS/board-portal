import { useEffect, useState } from "react";
import { fetchUserPhotoBlob } from "../api/userPhotos";

function initials(firstName: string, lastName: string): string {
  return `${firstName.charAt(0)}${lastName.charAt(0)}`.toUpperCase();
}

export function Avatar({
  userId,
  photoUpdatedAt,
  firstName,
  lastName,
}: {
  userId?: string;
  photoUpdatedAt?: string | null;
  firstName: string;
  lastName: string;
}) {
  const [photoSrc, setPhotoSrc] = useState<string | null>(null);

  useEffect(() => {
    if (!userId || !photoUpdatedAt) {
      setPhotoSrc(null);
      return;
    }
    let objectUrl: string | null = null;
    let cancelled = false;
    fetchUserPhotoBlob(userId).then((blob) => {
      if (cancelled) return;
      objectUrl = URL.createObjectURL(blob);
      setPhotoSrc(objectUrl);
    });
    return () => {
      cancelled = true;
      if (objectUrl) URL.revokeObjectURL(objectUrl);
    };
  }, [userId, photoUpdatedAt]);

  if (photoSrc) {
    return <img className="avatar avatar-photo" src={photoSrc} alt="" />;
  }
  return <span className="avatar">{initials(firstName, lastName)}</span>;
}
