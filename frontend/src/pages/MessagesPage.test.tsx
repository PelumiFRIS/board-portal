import { render, screen } from "@testing-library/react";
import { describe, expect, it } from "vitest";
import { isSafeLinkUrl, renderMessageBody } from "./MessagesPage";

describe("renderMessageBody", () => {
  it("renders bold, italic, and strikethrough inline formatting", () => {
    render(<>{renderMessageBody("**bold** and *italic* and ~~struck~~")}</>);
    expect(screen.getByText("bold", { selector: "strong" })).toBeInTheDocument();
    expect(screen.getByText("italic", { selector: "em" })).toBeInTheDocument();
    expect(screen.getByText("struck", { selector: "del" })).toBeInTheDocument();
  });

  it("renders a bulleted list", () => {
    render(<>{renderMessageBody("- Agenda items\n- Approve Q3 budget")}</>);
    const list = screen.getByRole("list");
    expect(list.tagName).toBe("UL");
    expect(screen.getByText("Agenda items")).toBeInTheDocument();
    expect(screen.getByText("Approve Q3 budget")).toBeInTheDocument();
  });

  it("renders a numbered list", () => {
    render(<>{renderMessageBody("1. First\n2. Second")}</>);
    const list = screen.getByRole("list");
    expect(list.tagName).toBe("OL");
  });

  it("renders a blockquote for '> ' prefixed lines", () => {
    render(<>{renderMessageBody("> This is a quote test")}</>);
    const quote = document.querySelector("blockquote");
    expect(quote).not.toBeNull();
    expect(quote).toHaveTextContent("This is a quote test");
  });

  it("renders inline code spans", () => {
    render(<>{renderMessageBody("Run `npm test` before pushing")}</>);
    expect(screen.getByText("npm test", { selector: "code" })).toBeInTheDocument();
  });

  it("renders a safe https link as a real clickable anchor", () => {
    render(<>{renderMessageBody("[the board pack](https://example.com/board-pack)")}</>);
    const link = screen.getByRole("link", { name: "the board pack" });
    expect(link).toHaveAttribute("href", "https://example.com/board-pack");
    expect(link).toHaveAttribute("target", "_blank");
    expect(link).toHaveAttribute("rel", "noopener noreferrer");
  });

  it("renders a safe link whose URL contains a parenthesis", () => {
    render(<>{renderMessageBody("[disambiguation](https://en.wikipedia.org/wiki/Example_(disambiguation))")}</>);
    const link = screen.getByRole("link", { name: "disambiguation" });
    expect(link).toHaveAttribute("href", "https://en.wikipedia.org/wiki/Example_(disambiguation)");
  });

  it("renders a javascript: URL as plain text instead of a clickable link", () => {
    render(<>{renderMessageBody("[click me](javascript:alert(1))")}</>);
    expect(screen.queryByRole("link")).toBeNull();
    expect(screen.getByText("click me")).toBeInTheDocument();
  });
});

describe("isSafeLinkUrl", () => {
  it("allows http, https, and mailto URLs", () => {
    expect(isSafeLinkUrl("https://example.com")).toBe(true);
    expect(isSafeLinkUrl("http://example.com")).toBe(true);
    expect(isSafeLinkUrl("mailto:board@example.com")).toBe(true);
  });

  it("rejects javascript: and data: URLs", () => {
    expect(isSafeLinkUrl("javascript:alert(1)")).toBe(false);
    expect(isSafeLinkUrl("data:text/html,<script>alert(1)</script>")).toBe(false);
  });

  it("is tolerant of leading whitespace before a safe scheme", () => {
    expect(isSafeLinkUrl("  https://example.com")).toBe(true);
  });
});
