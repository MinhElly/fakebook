import { useEffect, useState } from "react"

import api from "@/services/apis"

import { useAuth } from "@/providers/AuthProvider"

import Skeleton from "@/components/ui/Skeleton"

type Props = Omit<React.ImgHTMLAttributes<HTMLImageElement>, "src"> & {
  src: string
  alt: string
}

export default function MediaImage({
  src,
  alt,
  className = "",
  ...props
}: Props) {
  const { status } = useAuth()

  const key = `${status}:${src}`

  const [result, setResult] = useState<{
    key: string
    url: string | null
    error: boolean
  }>({ key: "", url: null, error: false })

  const [loadedKey, setLoadedKey] = useState("")

  const [failedKey, setFailedKey] = useState("")

  const [fallbackKey, setFallbackKey] = useState("")

  useEffect(() => {
    setLoadedKey("")
    setFailedKey("")
    setFallbackKey("")

    const controller = new AbortController()

    let objectUrl: string | undefined

    if (
      status !== "authenticated" ||
      !src.includes("/services/mediaservice/")
    ) {
      setResult({ key, url: src, error: !src })

      return () => controller.abort()
    }

    setResult({ key, url: null, error: false })

    api
      .get(src, { responseType: "blob", signal: controller.signal })
      .then((response) => {
        objectUrl = URL.createObjectURL(response.data)

        if (controller.signal.aborted) URL.revokeObjectURL(objectUrl)
        else setResult({ key, url: objectUrl, error: false })
      })
      .catch(() => {
        if (!controller.signal.aborted)
          setResult({ key, url: null, error: true })
      })

    return () => {
      controller.abort()
      if (objectUrl) URL.revokeObjectURL(objectUrl)
    }
  }, [src, status, key])

  const active = result.key === key

  const error = (active && result.error) || failedKey === key

  const loaded = loadedKey === key

  const hasSize = /(?:^|\s)(?:[a-z]+:)?(?:h-(?!full|auto)|size-|aspect-)/.test(
    className,
  )

  const imageFit = className
    .split(/\s+/)
    .filter((token) => /^(?:[a-z]+:)?object-/.test(token))
    .join(" ")

  const fixedHeight = /(?:^|\s)(?:[a-z]+:)?(?:h-(?!auto)|size-)/.test(className)

  return (
    <span
      className={`relative inline-block overflow-hidden align-middle ${className} ${
        !hasSize && !loaded ? "aspect-video" : ""
      }`}
      style={props.style}
      aria-busy={!loaded && !error}
    >
      {!loaded && !error && (
        <span
          role="status"
          aria-label={`Đang tải ảnh ${alt}`}
          className="absolute inset-0"
        >
          <Skeleton className="h-full w-full rounded-[inherit]" />
        </span>
      )}
      {error && (
        <span
          role="img"
          aria-label={`Không tải được ảnh ${alt}`}
          className="absolute inset-0 flex items-center justify-center bg-[#E4E6EB] text-[#65676B]"
        >
          <svg
            aria-hidden="true"
            className="max-h-6 max-w-6"
            viewBox="0 0 24 24"
            fill="none"
            stroke="currentColor"
          >
            <path d="M3 3h18v18H3zM3 17l6-6 4 4 3-3 5 5" />
          </svg>
        </span>
      )}
      {active && result.url && !error && (
        <img
          {...props}
          src={result.url}
          alt={alt}
          className={`block w-full ${
            fixedHeight ? "h-full" : "h-auto"
          } ${imageFit} ${loaded ? "" : "opacity-0"}`}
          style={{ ...props.style, maxHeight: "inherit" }}
          onLoad={(event) => {
            setLoadedKey(key)
            props.onLoad?.(event)
          }}
          onError={(event) => {
            if (props.onError && fallbackKey !== key) {
              const previous = event.currentTarget.src

              props.onError(event)

              if (event.currentTarget.src === previous) setFailedKey(key)
              else setFallbackKey(key)
            } else setFailedKey(key)
          }}
        />
      )}
    </span>
  )
}
