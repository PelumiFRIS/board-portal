import { AxiosError } from "axios";
import { describe, expect, it } from "vitest";
import { extractErrorMessage } from "./client";

function axiosErrorWithData(data: unknown): AxiosError {
  const error = new AxiosError("Request failed", "ERR_BAD_REQUEST");
  // @ts-expect-error -- only the fields extractErrorMessage reads are needed for this test
  error.response = { status: 400, statusText: "Bad Request", headers: {}, data };
  return error;
}

describe("extractErrorMessage", () => {
  it("prefers fieldErrors when present, joined with a comma", () => {
    const error = axiosErrorWithData({ fieldErrors: ["Title is required", "Category is required"] });
    expect(extractErrorMessage(error)).toBe("Title is required, Category is required");
  });

  it("falls back to the message field when there are no fieldErrors", () => {
    const error = axiosErrorWithData({ message: "Cannot message a disabled member" });
    expect(extractErrorMessage(error)).toBe("Cannot message a disabled member");
  });

  it("falls back to a generic message when the response body has neither", () => {
    const error = axiosErrorWithData({});
    expect(extractErrorMessage(error)).toBe("Something went wrong. Please try again.");
  });

  it("falls back to a generic message for a non-axios error", () => {
    expect(extractErrorMessage(new Error("network exploded"))).toBe("Something went wrong. Please try again.");
  });
});
