import React, { useEffect, useRef } from 'react';
import { useMapsLibrary } from '@vis.gl/react-google-maps';
import { Input } from '@/components/ui/input';
import { Search } from 'lucide-react';

interface MapSearchProps {
  onPlaceSelect: (place: google.maps.places.PlaceResult) => void;
  mapRef: React.MutableRefObject<google.maps.Map | null>;
}

export function MapSearch({ onPlaceSelect, mapRef }: MapSearchProps) {
  const inputRef = useRef<HTMLInputElement>(null);
  const placesLibrary = useMapsLibrary('places');
  const autocompleteRef = useRef<google.maps.places.Autocomplete | null>(null);

  useEffect(() => {
    if (!placesLibrary || !inputRef.current) return;

    autocompleteRef.current = new placesLibrary.Autocomplete(inputRef.current, {
      fields: ['geometry', 'name', 'formatted_address'],
    });

    // Bias results toward current map viewport
    const updateBounds = () => {
      if (autocompleteRef.current && mapRef.current) {
        const bounds = mapRef.current.getBounds();
        if (bounds) autocompleteRef.current.setBounds(bounds);
      }
    };
    updateBounds();

    const listener = autocompleteRef.current.addListener('place_changed', () => {
      if (autocompleteRef.current) {
        const place = autocompleteRef.current.getPlace();
        if (place.geometry?.location) {
          onPlaceSelect(place);
          // Clear input after navigation
          if (inputRef.current) inputRef.current.value = '';
        }
      }
    });

    return () => {
      google.maps.event.removeListener(listener);
      if (autocompleteRef.current) {
        google.maps.event.clearInstanceListeners(autocompleteRef.current);
      }
    };
  }, [placesLibrary, onPlaceSelect, mapRef]);

  return (
    <div className="relative shadow-lg rounded-full bg-background/95 backdrop-blur-sm overflow-hidden border border-border">
      <div className="absolute inset-y-0 left-0 flex items-center pl-4 pointer-events-none text-muted-foreground">
        <Search className="w-4 h-4" />
      </div>
      <Input
        ref={inputRef}
        type="text"
        placeholder="Search for a location…"
        className="w-full h-11 pl-11 pr-4 bg-transparent border-none rounded-full shadow-none focus-visible:ring-0 text-sm"
      />
    </div>
  );
}
