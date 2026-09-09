import { act, fireEvent, render, screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import { ToastProvider, useToast } from "./ToastContext";

function Consumer() {
  const toast = useToast();
  return (
    <div>
      <button onClick={() => toast.success("Profile updated.")}>Trigger success</button>
      <button onClick={() => toast.error("Something went wrong.")}>Trigger error</button>
    </div>
  );
}

function renderConsumer() {
  return render(
    <ToastProvider>
      <Consumer />
    </ToastProvider>,
  );
}

describe("ToastContext", () => {
  it("shows a success toast with the right message and styling", async () => {
    const user = userEvent.setup();
    renderConsumer();
    await user.click(screen.getByText("Trigger success"));

    const toast = screen.getByText("Profile updated.").closest(".toast");
    expect(toast).not.toBeNull();
    expect(toast).toHaveClass("toast-success");
  });

  it("shows an error toast with the right message and styling", async () => {
    const user = userEvent.setup();
    renderConsumer();
    await user.click(screen.getByText("Trigger error"));

    const toast = screen.getByText("Something went wrong.").closest(".toast");
    expect(toast).not.toBeNull();
    expect(toast).toHaveClass("toast-error");
  });

  it("dismisses a toast immediately when clicked", async () => {
    const user = userEvent.setup();
    renderConsumer();
    await user.click(screen.getByText("Trigger success"));
    expect(screen.getByText("Profile updated.")).toBeInTheDocument();

    await user.click(screen.getByText("Profile updated.").closest(".toast")!);
    expect(screen.queryByText("Profile updated.")).toBeNull();
  });

  describe("auto-dismiss", () => {
    beforeEach(() => {
      vi.useFakeTimers();
    });

    afterEach(() => {
      vi.useRealTimers();
    });

    it("removes a toast on its own after the timeout", () => {
      renderConsumer();
      fireEvent.click(screen.getByText("Trigger success"));
      expect(screen.getByText("Profile updated.")).toBeInTheDocument();

      act(() => {
        vi.advanceTimersByTime(3500);
      });
      expect(screen.queryByText("Profile updated.")).toBeNull();
    });
  });
});
