import React, { useState, useCallback } from 'react';
import { useMapsLibrary } from '@vis.gl/react-google-maps';
import { Input } from '@/components/ui/input';
import { Search, Loader2 } from 'lucide-react';
import { toast } from 'sonner';

interface MapSearchProps {
  onPlaceSelect: (place: google.maps.places.PlaceResult) => void;
  mapRef: React.MutableRefObject<google.maps.Map | null>;
}

export function MapSearch({ onPlaceSelect, mapRef }: MapSearchProps) {
  const [query, setQuery] = useState('');
  const [loading, setLoading] = useState(false);

  // Use Geocoder — built into Maps JS API, no separate Places API needed
  const geocodingLibrary = useMapsLibrary('geocoding');

  const handleSearch = useCallback(async () => {
    if (!query.trim() || !geocodingLibrary) return;

    setLoading(true);
    try {
      const geocoder = new geocodingLibrary.Geocoder();

      // Bias results toward current map viewport when available
      const bounds = mapRef.current?.getBounds() ?? undefined;

      const { results } = await geocoder.geocode({
        address: query,
        bounds: bounds,
      });

      if (results.length === 0) {
        toast.error('Location not found. Try a different search term.');
        return;
      }

      const result = results[0];
      // Reuse the same callback shape Home.tsx expects
      onPlaceSelect({
        geometry: { location: result.geometry.location } as google.maps.places.PlaceGeometry,
        name: result.formatted_address,
        formatted_address: result.formatted_address,
      });
      setQuery('');
    } catch (err: any) {
      if (err?.code === 'ZERO_RESULTS' || err?.message?.includes('ZERO_RESULTS')) {
        toast.error('Location not found. Try a different search term.');
      } else {
        toast.error('Search failed. Please try again.');
      }
    } finally {
      setLoading(false);
    }
  }, [query, geocodingLibrary, onPlaceSelect, mapRef]);

  const handleKeyDown = (e: React.KeyboardEvent<HTMLInputElement>) => {
    if (e.key === 'Enter') {
      e.preventDefault();
      handleSearch();
    }
  };

  return (
    <div className="relative shadow-lg rounded-full bg-background/95 backdrop-blur-sm border border-border flex items-center overflow-hidden">
      <div className="absolute inset-y-0 left-0 flex items-center pl-4 pointer-events-none text-muted-foreground">
        {loading ? (
          <Loader2 className="w-4 h-4 animate-spin" />
        ) : (
          <Search className="w-4 h-4" />
        )}
      </div>
      <Input
        type="text"
        value={query}
        onChange={(e) => setQuery(e.target.value)}
        onKeyDown={handleKeyDown}
        placeholder="Search a location and press Enter…"
        className="w-full h-11 pl-11 pr-16 bg-transparent border-none rounded-full shadow-none focus-visible:ring-0 text-sm"
        disabled={loading}
      />
      <button
        onClick={handleSearch}
        disabled={loading || !query.trim()}
        className="absolute right-2 h-7 px-3 rounded-full bg-primary text-primary-foreground text-xs font-semibold disabled:opacity-40 hover:bg-primary/90 transition-colors"
      >
        Go
      </button>
    </div>
  );
}
