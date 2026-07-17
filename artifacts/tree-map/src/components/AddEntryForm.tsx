import React, { useEffect } from 'react';
import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import * as z from 'zod';
import {
  useCreateEntry,
  getListEntriesQueryKey,
  getGetEntryStatsQueryKey,
  EntryInputCategory,
} from '@workspace/api-client-react';
import { useQueryClient } from '@tanstack/react-query';
import { Card, CardContent, CardHeader, CardTitle, CardDescription, CardFooter } from '@/components/ui/card';
import { Form, FormControl, FormField, FormItem, FormLabel, FormMessage } from '@/components/ui/form';
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from '@/components/ui/select';
import { Input } from '@/components/ui/input';
import { Textarea } from '@/components/ui/textarea';
import { Button } from '@/components/ui/button';
import { X, Loader2, MapPin, TreePine, Droplets, Trash2 } from 'lucide-react';
import { CATEGORY_LABELS, CATEGORY_COLORS } from './MapComponent';
import { toast } from 'sonner';

const REPORTER_KEY = 'field-journal:reporter-name';

const formSchema = z.object({
  category: z.nativeEnum(EntryInputCategory, {
    required_error: 'Please select a category.',
  }),
  reporter: z.string().min(1, 'Reporter name is required').max(100),
  notes: z.string().optional(),
});

type FormValues = z.infer<typeof formSchema>;

const CATEGORY_FORM_ICONS: Record<string, React.ReactNode> = {
  tree_planted: <TreePine className="w-4 h-4" />,
  mangrove_dying: <Droplets className="w-4 h-4" />,
  trash_pile: <Trash2 className="w-4 h-4" />,
};

interface AddEntryFormProps {
  lat: number;
  lng: number;
  onClose: () => void;
  onSuccess: () => void;
}

export function AddEntryForm({ lat, lng, onClose, onSuccess }: AddEntryFormProps) {
  const queryClient = useQueryClient();
  const createEntry = useCreateEntry();

  const form = useForm<FormValues>({
    resolver: zodResolver(formSchema),
    defaultValues: {
      reporter: '',
      notes: '',
    },
  });

  // Restore saved reporter name
  useEffect(() => {
    const saved = localStorage.getItem(REPORTER_KEY);
    if (saved) form.setValue('reporter', saved);
  }, [form]);

  const onSubmit = (values: FormValues) => {
    // Persist reporter name
    localStorage.setItem(REPORTER_KEY, values.reporter);

    createEntry.mutate(
      {
        data: {
          lat,
          lng,
          category: values.category,
          reporter: values.reporter,
          notes: values.notes,
        },
      },
      {
        onSuccess: () => {
          queryClient.invalidateQueries({ queryKey: getListEntriesQueryKey() });
          queryClient.invalidateQueries({ queryKey: getGetEntryStatsQueryKey() });
          toast.success(`${CATEGORY_LABELS[values.category]} logged successfully!`);
          onSuccess();
        },
        onError: () => {
          toast.error('Failed to save entry. Please try again.');
        },
      }
    );
  };

  return (
    <Card className="w-80 shadow-2xl border-border animate-in fade-in slide-in-from-bottom-4 duration-200">
      <CardHeader className="pb-3 relative">
        <Button
          variant="ghost"
          size="icon"
          className="absolute right-2 top-2 h-8 w-8 text-muted-foreground hover:text-foreground"
          onClick={onClose}
          type="button"
        >
          <X className="w-4 h-4" />
        </Button>
        <CardTitle className="font-serif text-lg">Log Observation</CardTitle>
        <CardDescription className="flex items-center gap-1.5 text-xs font-mono">
          <MapPin className="w-3 h-3 text-primary flex-shrink-0" />
          {lat.toFixed(5)}, {lng.toFixed(5)}
        </CardDescription>
      </CardHeader>

      <CardContent className="pt-0">
        <Form {...form}>
          <form id="add-entry-form" onSubmit={form.handleSubmit(onSubmit)} className="space-y-3">
            <FormField
              control={form.control}
              name="category"
              render={({ field }) => (
                <FormItem>
                  <FormLabel>Category</FormLabel>
                  <Select onValueChange={field.onChange} defaultValue={field.value}>
                    <FormControl>
                      <SelectTrigger>
                        <SelectValue placeholder="Select type…" />
                      </SelectTrigger>
                    </FormControl>
                    <SelectContent>
                      {Object.entries(CATEGORY_LABELS).map(([value, label]) => (
                        <SelectItem key={value} value={value}>
                          <div className="flex items-center gap-2">
                            <div
                              className="w-5 h-5 rounded-full flex items-center justify-center text-white flex-shrink-0"
                              style={{ backgroundColor: CATEGORY_COLORS[value] }}
                            >
                              <span style={{ transform: 'scale(0.7)', display: 'flex' }}>
                                {CATEGORY_FORM_ICONS[value]}
                              </span>
                            </div>
                            {label}
                          </div>
                        </SelectItem>
                      ))}
                    </SelectContent>
                  </Select>
                  <FormMessage />
                </FormItem>
              )}
            />

            <FormField
              control={form.control}
              name="reporter"
              render={({ field }) => (
                <FormItem>
                  <FormLabel>Your Name</FormLabel>
                  <FormControl>
                    <Input placeholder="Jane Doe" {...field} />
                  </FormControl>
                  <FormMessage />
                </FormItem>
              )}
            />

            <FormField
              control={form.control}
              name="notes"
              render={({ field }) => (
                <FormItem>
                  <FormLabel>Field Notes <span className="text-muted-foreground font-normal">(optional)</span></FormLabel>
                  <FormControl>
                    <Textarea
                      placeholder="Describe what you observe…"
                      className="resize-none h-20 text-sm"
                      {...field}
                    />
                  </FormControl>
                  <FormMessage />
                </FormItem>
              )}
            />
          </form>
        </Form>
      </CardContent>

      <CardFooter className="flex gap-2 pt-2">
        <Button variant="outline" onClick={onClose} type="button" className="flex-1">
          Cancel
        </Button>
        <Button type="submit" form="add-entry-form" disabled={createEntry.isPending} className="flex-1">
          {createEntry.isPending && <Loader2 className="w-4 h-4 mr-2 animate-spin" />}
          Save Entry
        </Button>
      </CardFooter>
    </Card>
  );
}
