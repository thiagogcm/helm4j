package bridge

import (
	"encoding/json"
	"fmt"
	"testing"
)

func TestEncodeErrorAlwaysReturnsValidJSON(t *testing.T) {
	payload := EncodeError("runSearch", fmt.Errorf("bad \"quote\"\nline"))

	decoded := map[string]string{}
	if err := json.Unmarshal([]byte(payload), &decoded); err != nil {
		t.Fatalf("expected valid JSON, got: %v\npayload: %s", err, payload)
	}

	if decoded["stage"] != "runSearch" {
		t.Fatalf("expected stage runSearch, got %q", decoded["stage"])
	}
	if decoded["error"] == "" {
		t.Fatal("expected non-empty error message")
	}
}

func TestEncodeErrorWithContext(t *testing.T) {
	payload := EncodeError("runShow", fmt.Errorf("boom"), "mode", "all", "chartRef", "repo/demo")

	decoded := map[string]string{}
	if err := json.Unmarshal([]byte(payload), &decoded); err != nil {
		t.Fatalf("expected valid JSON, got: %v", err)
	}

	if decoded["mode"] != "all" {
		t.Fatalf("expected mode all, got %q", decoded["mode"])
	}
	if decoded["chartRef"] != "repo/demo" {
		t.Fatalf("expected chartRef repo/demo, got %q", decoded["chartRef"])
	}
	if decoded["stage"] != "runShow" {
		t.Fatalf("expected stage runShow, got %q", decoded["stage"])
	}
}

func TestParseOptionsEmptyInput(t *testing.T) {
	type dummy struct {
		Name string `json:"name"`
	}

	opts, err := ParseOptions[dummy]("")
	if err != nil {
		t.Fatalf("unexpected error for empty input: %v", err)
	}
	if opts.Name != "" {
		t.Fatalf("expected zero value, got %q", opts.Name)
	}

	opts, err = ParseOptions[dummy]("   ")
	if err != nil {
		t.Fatalf("unexpected error for whitespace input: %v", err)
	}
	if opts.Name != "" {
		t.Fatalf("expected zero value, got %q", opts.Name)
	}
}

func TestParseOptionsValidJSON(t *testing.T) {
	type dummy struct {
		Name string `json:"name"`
		Age  int    `json:"age"`
	}

	opts, err := ParseOptions[dummy](`{"name":"test","age":42}`)
	if err != nil {
		t.Fatalf("unexpected error: %v", err)
	}
	if opts.Name != "test" || opts.Age != 42 {
		t.Fatalf("unexpected result: %+v", opts)
	}
}

func TestParseOptionsInvalidJSON(t *testing.T) {
	type dummy struct{}
	_, err := ParseOptions[dummy]("{bad json")
	if err == nil {
		t.Fatal("expected error for invalid JSON")
	}
}

func TestValidateWaitStrategy(t *testing.T) {
	cases := []struct {
		input   string
		wantErr bool
	}{
		{"", false},
		{"watcher", false},
		{"legacy", false},
		{"hookOnly", false},
		{"WATCHER", true},
		{"hook-only", true},
		{"unknown", true},
	}
	for _, tc := range cases {
		t.Run(tc.input, func(t *testing.T) {
			err := ValidateWaitStrategy(tc.input)
			if tc.wantErr && err == nil {
				t.Fatalf("expected error for input %q, got nil", tc.input)
			}
			if !tc.wantErr && err != nil {
				t.Fatalf("unexpected error for input %q: %v", tc.input, err)
			}
		})
	}
}

func TestMarshalJSONRoundTrip(t *testing.T) {
	type payload struct {
		Value string `json:"value"`
	}

	s, err := MarshalJSON(payload{Value: "hello"})
	if err != nil {
		t.Fatalf("unexpected error: %v", err)
	}

	var decoded payload
	if err := json.Unmarshal([]byte(s), &decoded); err != nil {
		t.Fatalf("roundtrip failed: %v", err)
	}
	if decoded.Value != "hello" {
		t.Fatalf("expected hello, got %q", decoded.Value)
	}
}
