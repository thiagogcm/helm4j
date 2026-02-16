package main

import (
	"log/slog"
	"os"
	"strconv"
	"strings"
)

var nativeLogLevel = new(slog.LevelVar)

var nativeLogger = slog.New(
	slog.NewTextHandler(os.Stderr, &slog.HandlerOptions{
		Level: nativeLogLevel,
		ReplaceAttr: func(_ []string, attr slog.Attr) slog.Attr {
			if attr.Key == slog.TimeKey {
				return slog.Attr{}
			}
			return attr
		},
	}),
)

func init() {
	setNativeLogLevelFromEnv()
}

func setNativeLogLevelFromEnv() bool {
	raw := strings.TrimSpace(os.Getenv("HELM_DEBUG"))
	if raw == "" {
		nativeLogLevel.Set(slog.LevelWarn)
		return false
	}

	debugEnabled, err := strconv.ParseBool(raw)
	if err != nil {
		nativeLogLevel.Set(slog.LevelWarn)
		nativeLogger.Warn(
			"invalid HELM_DEBUG value; defaulting to false",
			slog.String("value", raw),
			slog.Any("error", err),
		)
		return false
	}

	if debugEnabled {
		nativeLogLevel.Set(slog.LevelDebug)
		return true
	}

	nativeLogLevel.Set(slog.LevelWarn)
	return false
}
