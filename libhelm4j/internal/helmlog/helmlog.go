// Package helmlog provides structured logging for the libhelm4j native library.
//
// It wraps [log/slog] with Helm-aware defaults: timestamps are stripped from
// output (the caller controls when each line is produced), and the log level
// is driven by the HELM_DEBUG environment variable.
package helmlog

import (
	"log/slog"
	"os"
	"strconv"
	"strings"
)

// level is the global log level, adjustable at runtime via SetLevelFromEnv.
var level = new(slog.LevelVar)

// handler is the base slog handler writing to stderr with timestamps stripped.
var handler slog.Handler = slog.NewTextHandler(os.Stderr, &slog.HandlerOptions{
	Level: level,
	ReplaceAttr: func(_ []string, attr slog.Attr) slog.Attr {
		if attr.Key == slog.TimeKey {
			return slog.Attr{}
		}
		return attr
	},
})

// logger is the package-level logger shared across all internal packages.
var logger = slog.New(handler)

func init() {
	SetLevelFromEnv()
}

// Logger returns the shared [slog.Logger] for the native library.
func Logger() *slog.Logger { return logger }

// Handler returns the underlying [slog.Handler], useful for passing to Helm's
// action configuration which accepts a handler directly.
func Handler() slog.Handler { return handler }

// With returns a new [slog.Logger] that includes the given attributes in every
// log record. Use it to scope a logger to a particular operation.
func With(args ...any) *slog.Logger { return logger.With(args...) }

// SetLevelFromEnv reads the HELM_DEBUG environment variable and adjusts the
// global log level accordingly. It returns true when debug logging was enabled.
//
// A truthy HELM_DEBUG (e.g. "true", "1") sets [slog.LevelDebug]; anything else
// falls back to [slog.LevelWarn].
func SetLevelFromEnv() bool {
	raw := strings.TrimSpace(os.Getenv("HELM_DEBUG"))
	if raw == "" {
		level.Set(slog.LevelWarn)
		return false
	}

	debugEnabled, err := strconv.ParseBool(raw)
	if err != nil {
		level.Set(slog.LevelWarn)
		logger.Warn(
			"invalid HELM_DEBUG value; defaulting to false",
			slog.String("value", raw),
			slog.Any("error", err),
		)
		return false
	}

	if debugEnabled {
		level.Set(slog.LevelDebug)
		return true
	}

	level.Set(slog.LevelWarn)
	return false
}

// Level returns the current log level. Useful in tests.
func Level() slog.Level { return level.Level() }
