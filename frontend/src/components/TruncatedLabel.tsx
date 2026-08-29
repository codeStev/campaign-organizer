import { ComponentProps } from 'react';
import { Tooltip, TooltipContent, TooltipTrigger } from './ui/tooltip';

interface Props extends ComponentProps<'span'> {
  /** Full text shown in the tooltip; also what index.css truncates. */
  label: string;
}

/** A sidebar-row name that truncates with an ellipsis (via .article-link > span
 * in index.css) and shows the untruncated text in a tooltip on hover/focus. */
export function TruncatedLabel({ label, children, ...spanProps }: Props) {
  return (
    <Tooltip>
      <TooltipTrigger asChild>
        <span {...spanProps}>{children}</span>
      </TooltipTrigger>
      <TooltipContent>{label}</TooltipContent>
    </Tooltip>
  );
}
