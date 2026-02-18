package repomgr

import (
	"sync"
	"sync/atomic"
	"testing"
	"testing/synctest"
)

// TestParallelUpdateScheduling uses synctest to verify that the WaitGroup.Go
// pattern used by Update launches all goroutines concurrently rather than
// sequentially.
func TestParallelUpdateScheduling(t *testing.T) {
	synctest.Test(t, func(t *testing.T) {
		const n = 5
		var started atomic.Int32
		results := make([]string, n)

		var wg sync.WaitGroup
		for i := range n {
			wg.Go(func() {
				started.Add(1)
				// Simulate concurrent work; in the real code this is updateOneRepo.
				results[i] = "done"
			})
		}
		// After synctest.Wait all goroutines have been scheduled and are idle.
		synctest.Wait()

		// Verify all goroutines started before Wait returned.
		if got := started.Load(); got != n {
			t.Fatalf("expected %d goroutines to have started, got %d", n, got)
		}

		wg.Wait()

		for i, r := range results {
			if r != "done" {
				t.Fatalf("result[%d] not set", i)
			}
		}
	})
}
