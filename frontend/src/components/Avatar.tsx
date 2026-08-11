function initials(firstName: string, lastName: string): string {
  return `${firstName.charAt(0)}${lastName.charAt(0)}`.toUpperCase();
}

export function Avatar({ firstName, lastName }: { firstName: string; lastName: string }) {
  return <span className="avatar">{initials(firstName, lastName)}</span>;
}
