import React, { useEffect, useRef } from 'react';
import { useMapsLibrary } from '@vis.gl/react-google-maps';
import { Input } from '@/components/ui/input';
import { Search } from 'lucide-react';

interface MapSearchProps {
  onPlaceSelect: (place: google.maps.places.PlaceResult) => void;
}

export function MapSearch({ onPlaceSelect }: MapSearchProps) {
  const inputRef = useRef<HTMLInputElement>(null);
  const placesLibrary = useMapsLibrary('places');
  const autocompleteRef = useRef<google.maps.places.Autocomplete | null>(null);

  useEffect(() => {
    if (!placesLibrary || !inputRef.current) return;

    autocompleteRef.current = new placesLibrary.Autocomplete(inputRef.current, {
      fields: ['geometry', 'name', 'formatted_address']
    });

    autocompleteRef.current.addListener('place_changed', () => {
      if (autocompleteRef.current) {
        const place = autocompleteRef.current.getPlace();
        if (place.geometry?.location) {
          onPlaceSelect(place);
        }
      }
    });

    return () => {
      if (autocompleteRef.current) {
        google.maps.event.clearInstanceListeners(autocompleteRef.current);
      }
    };
  }, [placesLibrary, onPlaceSelect]);

  return (
    <div className="relative shadow-lg rounded-full bg-background overflow-hidden border border-border">
      <div className="absolute inset-y-0 left-0 flex items-center pl-4 pointer-events-none text-muted-foreground">
        <Search className="w-5 h-5" />
      </div>
      <Input
        ref={inputRef}
        type="text"
        placeholder="Search for a location..."
        className="w-[400px] h-12 pl-12 pr-4 bg-transparent border-none rounded-full shadow-none focus-visible:ring-0 text-base"
      />
    </div>
  );
}
