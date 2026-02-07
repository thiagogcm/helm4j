package main

import (
	"encoding/json"
	"fmt"
	"testing"

	"helm.sh/helm/v4/pkg/action"
)

func TestEncodeSearchErrorAlwaysReturnsValidJSON(t *testing.T) {
	payload := encodeSearchError("runSearch", fmt.Errorf("bad \"quote\"\nline"))

	decoded := map[string]string{}
	if err := json.Unmarshal([]byte(payload), &decoded); err != nil {
		t.Fatalf("expected valid JSON payload, got error: %v", err)
	}

	if decoded["stage"] != "runSearch" {
		t.Fatalf("expected stage runSearch, got %q", decoded["stage"])
	}
	if decoded["error"] == "" {
		t.Fatal("expected non-empty error message")
	}
}

func TestEncodeShowErrorAlwaysReturnsValidJSON(t *testing.T) {
	payload := encodeShowError(action.ShowAll, "repo/demo", "runShow", fmt.Errorf("boom \"x\""))

	decoded := map[string]string{}
	if err := json.Unmarshal([]byte(payload), &decoded); err != nil {
		t.Fatalf("expected valid JSON payload, got error: %v", err)
	}

	if decoded["mode"] != "all" {
		t.Fatalf("expected mode all, got %q", decoded["mode"])
	}
	if decoded["stage"] != "runShow" {
		t.Fatalf("expected stage runShow, got %q", decoded["stage"])
	}
	if decoded["error"] == "" {
		t.Fatal("expected non-empty error message")
	}
}
