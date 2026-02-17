package search

import (
	"fmt"
	"strings"
)

// Mode selects which helm search backend is used.
type Mode string

const (
	ModeRepo Mode = "repo"
	ModeHub  Mode = "hub"
)

func ParseMode(raw string) (Mode, error) {
	switch strings.ToLower(strings.TrimSpace(raw)) {
	case string(ModeRepo):
		return ModeRepo, nil
	case string(ModeHub):
		return ModeHub, nil
	default:
		return "", fmt.Errorf("unsupported search mode: %s", raw)
	}
}

func (m Mode) String() string {
	return string(m)
}

// Run executes helm search for the given mode.
func Run(mode Mode, opts Options) ([]Result, error) {
	switch mode {
	case ModeRepo:
		return runRepo(opts)
	case ModeHub:
		return runHub(opts)
	default:
		return nil, fmt.Errorf("unsupported search mode: %s", mode)
	}
}
