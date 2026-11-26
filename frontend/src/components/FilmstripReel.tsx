import { useEffect, useMemo, useRef, useState } from 'react';
import { Star } from 'lucide-react';
import type { MediaItemResponse } from '../types/api';

interface FilmstripReelProps {
  candidates: MediaItemResponse[];
  winner: MediaItemResponse;
  spinKey: number;
  onSpinComplete: () => void;
}

const CELL_WIDTH = 176;
const CELL_GAP = 8;
const STEP = CELL_WIDTH + CELL_GAP;
const WINNER_INDEX = 18;
const TOTAL_FRAMES = 24;

export function FilmstripReel({ candidates, winner, spinKey, onSpinComplete }: FilmstripReelProps) {
  const containerRef = useRef<HTMLDivElement>(null);
  const [offsetX, setOffsetX] = useState(0);
  const [animating, setAnimating] = useState(false);

  const frames = useMemo(() => {
    const pool = candidates.length > 0 ? candidates : [winner];
    const strip: MediaItemResponse[] = [];
    for (let i = 0; i < TOTAL_FRAMES; i++) {
      if (i === WINNER_INDEX) {
        strip.push(winner);
      } else {
        const pick = pool[Math.floor(Math.random() * pool.length)];
        strip.push(pick);
      }
    }
    return strip;
  }, [candidates, winner, spinKey]);

  useEffect(() => {
    setAnimating(false);
    setOffsetX(0);

    const frameId = requestAnimationFrame(() => {
      const containerWidth = containerRef.current?.clientWidth ?? 480;
      const targetCenter = WINNER_INDEX * STEP + CELL_WIDTH / 2;
      const targetOffset = targetCenter - containerWidth / 2;
      setAnimating(true);
      setOffsetX(Math.max(0, targetOffset));
    });

    const timer = window.setTimeout(() => {
      onSpinComplete();
    }, 1750);

    return () => {
      cancelAnimationFrame(frameId);
      window.clearTimeout(timer);
    };
  }, [spinKey, onSpinComplete]);

  return (
    <div className="overflow-hidden rounded-md border-2 border-[#D946EF] bg-[#04000A] shadow-[0_0_24px_rgba(217,70,239,0.4)]">
      <div className="film-sprockets-x border-b border-purple-500/30" />

      <div ref={containerRef} className="relative overflow-hidden py-3">
        <div className="pointer-events-none absolute inset-y-0 left-1/2 z-20 w-0.5 -translate-x-1/2 bg-[#39FF14] shadow-[0_0_12px_#39FF14]" />
        <div className="pointer-events-none absolute left-1/2 top-0 z-20 -translate-x-1/2 text-[10px] font-black text-[#39FF14] drop-shadow-[0_0_6px_#39FF14]">
          ▼
        </div>
        <div className="pointer-events-none absolute inset-y-0 left-0 z-10 w-10 bg-gradient-to-r from-[#04000A] to-transparent" />
        <div className="pointer-events-none absolute inset-y-0 right-0 z-10 w-10 bg-gradient-to-l from-[#04000A] to-transparent" />

        <div
          className="flex"
          style={{
            gap: `${CELL_GAP}px`,
            transform: `translate3d(-${offsetX}px, 0, 0)`,
            transition: animating ? 'transform 1.65s cubic-bezier(0.08, 0.88, 0.12, 1)' : 'none',
          }}
        >
          {frames.map((item, idx) => {
            const isWinner = idx === WINNER_INDEX;
            const pocketColor =
              idx % 2 === 0
                ? 'border-[#39FF14]/60 text-[#39FF14]'
                : 'border-[#FF1E8A]/60 text-[#FF5CA8]';
            return (
              <div
                key={`${item.imdbId}-${idx}`}
                style={{ width: `${CELL_WIDTH}px` }}
                className={`shrink-0 rounded border-2 bg-[#13052B] p-2.5 ${
                  isWinner
                    ? 'border-[#39FF14] shadow-[0_0_16px_rgba(57,255,20,0.45)]'
                    : 'border-purple-500/40'
                }`}
              >
                <div className="mb-1 flex items-center justify-between text-[10px] font-mono font-bold">
                  <span className={`rounded border px-1 ${pocketColor}`}>
                    #{String(idx + 1).padStart(2, '0')}
                  </span>
                  {item.rating !== null && (
                    <span className="inline-flex items-center gap-0.5 text-[#FFE600]">
                      <Star className="h-2.5 w-2.5 fill-[#FFE600]" />
                      {item.rating}
                    </span>
                  )}
                </div>
                <div className="truncate font-display text-xs font-extrabold text-white">
                  {item.title}
                </div>
                <div className="truncate text-[10px] text-purple-300/75">
                  {item.releaseYear ?? '—'} &middot; {item.mediaType}
                </div>
              </div>
            );
          })}
        </div>
      </div>

      <div className="film-sprockets-x border-t border-purple-500/30" />
    </div>
  );
}
