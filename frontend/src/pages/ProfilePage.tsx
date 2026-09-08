import { useState, type FormEvent } from "react";
import { changePassword, updateUserProfile } from "../api/auth";
import { extractErrorMessage } from "../api/client";
import { deleteUserPhoto, uploadUserPhoto } from "../api/userPhotos";
import { Avatar } from "../components/Avatar";
import { Sidebar } from "../components/Sidebar";
import { TopBar } from "../components/TopBar";
import { useAuth } from "../context/AuthContext";

export function ProfilePage() {
  const { user, refreshUser } = useAuth();

  const [title, setTitle] = useState(user?.title ?? "");
  const [phone, setPhone] = useState(user?.phone ?? "");
  const [bio, setBio] = useState(user?.bio ?? "");
  const [photoFile, setPhotoFile] = useState<File | null>(null);
  const [savingProfile, setSavingProfile] = useState(false);
  const [profileError, setProfileError] = useState<string | null>(null);
  const [profileSaved, setProfileSaved] = useState(false);
  const [removingPhoto, setRemovingPhoto] = useState(false);

  const [currentPassword, setCurrentPassword] = useState("");
  const [newPassword, setNewPassword] = useState("");
  const [confirmPassword, setConfirmPassword] = useState("");
  const [changingPassword, setChangingPassword] = useState(false);
  const [passwordError, setPasswordError] = useState<string | null>(null);
  const [passwordSaved, setPasswordSaved] = useState(false);

  if (!user) return null;

  async function handleSaveProfile(event: FormEvent) {
    event.preventDefault();
    setProfileError(null);
    setProfileSaved(false);
    setSavingProfile(true);
    try {
      if (photoFile) {
        await uploadUserPhoto(user!.id, photoFile);
      }
      await updateUserProfile(user!.id, { title, phone, bio });
      await refreshUser();
      setPhotoFile(null);
      setProfileSaved(true);
    } catch (err) {
      setProfileError(extractErrorMessage(err));
    } finally {
      setSavingProfile(false);
    }
  }

  async function handleRemovePhoto() {
    setProfileError(null);
    setRemovingPhoto(true);
    try {
      await deleteUserPhoto(user!.id);
      await refreshUser();
    } catch (err) {
      setProfileError(extractErrorMessage(err));
    } finally {
      setRemovingPhoto(false);
    }
  }

  async function handleChangePassword(event: FormEvent) {
    event.preventDefault();
    setPasswordError(null);
    setPasswordSaved(false);
    if (newPassword !== confirmPassword) {
      setPasswordError("New password and confirmation don't match");
      return;
    }
    setChangingPassword(true);
    try {
      await changePassword({ currentPassword, newPassword });
      setCurrentPassword("");
      setNewPassword("");
      setConfirmPassword("");
      setPasswordSaved(true);
    } catch (err) {
      setPasswordError(extractErrorMessage(err));
    } finally {
      setChangingPassword(false);
    }
  }

  return (
    <div className="app-shell">
      <Sidebar />
      <main className="main-content">
        <TopBar />
        <div className="page-header">
          <h1>My profile</h1>
          <p>Manage your own details and sign-in credentials</p>
        </div>

        <section className="dashboard-section">
          <h2>Profile</h2>
          {profileError && <p className="form-error">{profileError}</p>}
          {profileSaved && <p className="form-success">Profile updated.</p>}
          <form className="add-user-form" onSubmit={handleSaveProfile}>
            <div className="name-cell">
              <Avatar userId={user.id} photoUpdatedAt={user.photoUpdatedAt} firstName={user.firstName} lastName={user.lastName} />
              <div>
                <strong>
                  {user.firstName} {user.lastName}
                </strong>
                <p className="table-hint">{user.email}</p>
              </div>
            </div>
            <label>
              Photo
              <input type="file" accept="image/*" onChange={(e) => setPhotoFile(e.target.files?.[0] ?? null)} />
            </label>
            {user.photoUpdatedAt && (
              <button type="button" className="secondary small" disabled={removingPhoto} onClick={handleRemovePhoto}>
                {removingPhoto ? "Removing..." : "Remove photo"}
              </button>
            )}
            <label>
              Title
              <input value={title} onChange={(e) => setTitle(e.target.value)} placeholder="e.g. Chairperson" />
            </label>
            <label>
              Phone
              <input value={phone} onChange={(e) => setPhone(e.target.value)} />
            </label>
            <label>
              Bio
              <input value={bio} onChange={(e) => setBio(e.target.value)} />
            </label>
            <button type="submit" disabled={savingProfile}>
              {savingProfile ? "Saving..." : "Save changes"}
            </button>
          </form>
        </section>

        <section className="dashboard-section">
          <h2>Change password</h2>
          {passwordError && <p className="form-error">{passwordError}</p>}
          {passwordSaved && <p className="form-success">Password changed.</p>}
          <form className="add-user-form" onSubmit={handleChangePassword}>
            <label>
              Current password
              <input
                type="password"
                value={currentPassword}
                onChange={(e) => setCurrentPassword(e.target.value)}
                required
              />
            </label>
            <label>
              New password
              <input
                type="password"
                value={newPassword}
                onChange={(e) => setNewPassword(e.target.value)}
                minLength={8}
                required
              />
            </label>
            <label>
              Confirm new password
              <input
                type="password"
                value={confirmPassword}
                onChange={(e) => setConfirmPassword(e.target.value)}
                minLength={8}
                required
              />
            </label>
            <button type="submit" disabled={changingPassword}>
              {changingPassword ? "Changing..." : "Change password"}
            </button>
          </form>
        </section>
      </main>
    </div>
  );
}
